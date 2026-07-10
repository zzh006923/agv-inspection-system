import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AiAssistantPanel from './AiAssistantPanel.vue'

vi.mock('element-plus', () => ({
  ElMessage: {
    warning: vi.fn(),
    error: vi.fn(),
    success: vi.fn()
  }
}))

vi.mock('../api/agv', () => ({
  aiApi: {
    chat: vi.fn(),
    taskReview: vi.fn(),
    flawReview: vi.fn()
  },
  unwrap: vi.fn(result => result?.data ?? result)
}))

const { aiApi } = await import('../api/agv')
const { ElMessage } = await import('element-plus')

const elementStubs = {
  'el-card': { template: '<div><slot /></div>' },
  'el-tag': { template: '<span><slot /></span>' },
  'el-scrollbar': { template: '<div><slot /></div>' },
  'el-empty': { props: ['description'], template: '<div class="empty">{{ description }}</div>' },
  'el-button': {
    props: ['disabled', 'loading'],
    emits: ['click'],
    template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>'
  },
  'el-input': {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template: '<textarea :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)"></textarea>'
  }
}

function mountPanel(extraProps = {}) {
  return mount(AiAssistantPanel, {
    props: {
      taskId: 1,
      task: { taskCode: 'TASK20260629', taskName: '隧道巡检', taskStatus: '待上传', startPos: 'A口', executor: '张三' },
      flaws: [{ id: 7, flawName: '裂缝异常', flawType: '裂缝', flawDistance: 12, confirmed: 0, remark: '待复核' }],
      sensorRecords: [{ sensorName: '温度传感器', sensorValue: '28℃', status: '正常' }],
      actionRecords: [{ deviceName: '补光灯', action: '开启', result: '成功' }],
      summary: { flawCount: 1, unconfirmedCount: 1, canUpload: false },
      selectedFlaw: { id: 7, flawName: '裂缝异常', flawType: '裂缝', level: '中', flawDistance: 12 },
      ...extraProps
    },
    global: { stubs: elementStubs }
  })
}

describe('AiAssistantPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    aiApi.chat.mockResolvedValue({ data: { answer: '建议先人工复核后再上传', conversationId: 'conv-1' } })
    aiApi.taskReview.mockResolvedValue({ data: { answer: '巡检复盘完成' } })
    aiApi.flawReview.mockResolvedValue({ data: { answer: '该故障需要现场确认' } })
  })

  it('输入问题后应调用 AI 自由问答接口，并展示 AI 回复', async () => {
    const wrapper = mountPanel()
    await wrapper.find('textarea').setValue('现在能上传吗？')
    await wrapper.findAll('button').at(-1).trigger('click')
    await flushPromises()

    expect(aiApi.chat).toHaveBeenCalledTimes(1)
    const payload = aiApi.chat.mock.calls[0][0]
    expect(payload.taskId).toBe(1)
    expect(payload.question).toBe('现在能上传吗？')
    expect(payload.context).toContain('TASK20260629')
    expect(payload.context).toContain('裂缝异常')
    expect(wrapper.text()).toContain('建议先人工复核后再上传')
  })

  it('点击预设问题时应调用 askPreset 分支并传入固定问题', async () => {
    const wrapper = mountPanel()
    const uploadButton = wrapper.findAll('button').find(btn => btn.text().includes('是否可上传'))
    await uploadButton.trigger('click')
    await flushPromises()

    expect(aiApi.chat).toHaveBeenCalledTimes(1)
    expect(aiApi.chat.mock.calls[0][0].question).toContain('这个任务现在能不能直接上传')
  })

  it('点击巡检复盘时应调用 taskReview 接口', async () => {
    const wrapper = mountPanel()
    const reviewButton = wrapper.findAll('button').find(btn => btn.text().includes('巡检复盘'))
    await reviewButton.trigger('click')
    await flushPromises()
    expect(aiApi.taskReview).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('巡检复盘完成')
  })

  it('选择故障后点击故障研判应调用 flawReview 接口', async () => {
    const wrapper = mountPanel()
    const flawButton = wrapper.findAll('button').find(btn => btn.text().includes('故障研判'))
    await flawButton.trigger('click')
    await flushPromises()

    expect(aiApi.flawReview).toHaveBeenCalledTimes(1)
    expect(aiApi.flawReview.mock.calls[0][0]).toBe(7)
    expect(wrapper.text()).toContain('该故障需要现场确认')
  })

  it('未选择故障时点击故障研判应给出提示，不调用接口', async () => {
    const wrapper = mountPanel({ selectedFlaw: {} })
    const flawButton = wrapper.findAll('button').find(btn => btn.text().includes('故障研判'))
    await flawButton.trigger('click')
    await flushPromises()
    expect(aiApi.flawReview).not.toHaveBeenCalled()
    expect(ElMessage.warning).toHaveBeenCalledWith('请先选择一条故障记录')
  })

  it('输入空问题时应提示用户，不调用 AI 接口', async () => {
    const wrapper = mountPanel()
    await wrapper.find('textarea').setValue('   ')
    await wrapper.findAll('button').at(-1).trigger('click')
    await flushPromises()

    expect(ElMessage.warning).toHaveBeenCalledWith('请输入问题')
    expect(aiApi.chat).not.toHaveBeenCalled()
  })

  it('接口返回空回答时应展示默认回复，并保留已有 conversationId', async () => {
    aiApi.chat
      .mockResolvedValueOnce({ data: { answer: '第一轮回复', conversationId: 'conv-1' } })
      .mockResolvedValueOnce({ data: { answer: '', conversationId: '' } })

    const wrapper = mountPanel()
    await wrapper.find('textarea').setValue('第一轮')
    await wrapper.findAll('button').at(-1).trigger('click')
    await flushPromises()

    await wrapper.find('textarea').setValue('第二轮')
    await wrapper.findAll('button').at(-1).trigger('click')
    await flushPromises()

    expect(aiApi.chat).toHaveBeenCalledTimes(2)
    expect(aiApi.chat.mock.calls[1][0].conversationId).toBe('conv-1')
    expect(wrapper.text()).toContain('AI暂无回复')
  })

  it('AI 接口异常时应展示错误提示并清空输入状态', async () => {
    aiApi.chat.mockRejectedValue(new Error('网络异常'))
    const wrapper = mountPanel()
    await wrapper.find('textarea').setValue('测试异常')
    await wrapper.findAll('button').at(-1).trigger('click')
    await flushPromises()

    expect(ElMessage.error).toHaveBeenCalledWith('网络异常')
    expect(wrapper.find('textarea').element.value).toBe('')
  })

  it('无故障、无传感器和无联动记录时应在上下文中写入暂无记录', async () => {
    const wrapper = mountPanel({
      task: {},
      flaws: [],
      sensorRecords: [],
      actionRecords: [],
      summary: {},
      selectedFlaw: null
    })
    await wrapper.find('textarea').setValue('生成巡检复盘')
    await wrapper.findAll('button').at(-1).trigger('click')
    await flushPromises()

    const context = aiApi.chat.mock.calls[0][0].context
    expect(context).toContain('暂无故障记录')
    expect(context).toContain('暂无传感器记录')
    expect(context).toContain('暂无联动记录')
    expect(context).toContain('未确认故障数：0')
  })
})
