import { describe, expect, it, vi, beforeEach } from 'vitest'
import { h } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import TaskFormDialog from './TaskFormDialog.vue'

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

vi.mock('../api/agv', () => ({
  taskApi: {
    nextCode: vi.fn(),
    add: vi.fn(),
    update: vi.fn()
  },
  unwrap: vi.fn(result => result?.data ?? result)
}))

const { taskApi } = await import('../api/agv')
const { ElMessage } = await import('element-plus')

let validateMock
const FormStub = {
  props: ['model', 'rules'],
  setup(props, { slots, expose }) {
    validateMock = vi.fn().mockResolvedValue(true)
    expose({ validate: validateMock })
    return () => h('form', {}, slots.default?.())
  }
}

const stubs = {
  'el-dialog': {
    props: ['modelValue', 'title'],
    template: '<div v-if="modelValue" class="dialog"><h2>{{ title }}</h2><slot /><slot name="footer" /></div>'
  },
  'el-form': FormStub,
  'el-form-item': { template: '<label><slot /></label>' },
  'el-input': {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template: '<span><input v-bind="$attrs" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" /><slot name="append" /></span>'
  },
  'el-button': {
    props: ['loading'],
    emits: ['click'],
    template: '<button type="button" :data-loading="loading" @click="$emit(\'click\')"><slot /></button>'
  },
  'el-checkbox': {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template: '<label><input class="auto-start" type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" /><slot /></label>'
  }
}

function mountDialog(props = {}) {
  return mount(TaskFormDialog, {
    props: { modelValue: false, mode: 'add', task: null, ...props },
    global: { stubs }
  })
}

async function fillRequiredFields(wrapper) {
  await wrapper.find('input[placeholder="如：1号线 K12+300-K12+800 区间巡检"]').setValue('1号线巡检')
  await wrapper.find('input[placeholder="如：A端风井入口"]').setValue('A端入口')
  await wrapper.find('input[placeholder="如：500m"]').setValue('500m')
  await wrapper.find('input[placeholder="运维管理员"]').setValue('管理员')
  await wrapper.find('input[placeholder="巡线车操作员"]').setValue('操作员')
}

async function clickSave(wrapper) {
  const saveButton = wrapper.findAll('button').find(btn => btn.text().includes('保存任务'))
  await saveButton.trigger('click')
  await flushPromises()
}

describe('TaskFormDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    taskApi.nextCode.mockResolvedValue({ data: 'TASK20260629001' })
    taskApi.add.mockResolvedValue({ data: { id: 9, taskCode: 'TASK20260629001', taskName: '1号线巡检' } })
    taskApi.update.mockResolvedValue({ data: { id: 3, taskCode: 'TASK-EDIT', taskName: '修改后的任务' } })
  })

  it('新增任务弹窗打开时应自动获取任务编号，并提交新增接口', async () => {
    const wrapper = mountDialog()
    await wrapper.setProps({ modelValue: true })
    await flushPromises()

    expect(taskApi.nextCode).toHaveBeenCalledTimes(1)
    await fillRequiredFields(wrapper)
    await clickSave(wrapper)

    expect(taskApi.add).toHaveBeenCalledTimes(1)
    expect(taskApi.add.mock.calls[0][0]).toMatchObject({
      taskCode: 'TASK20260629001',
      taskName: '1号线巡检',
      startPos: 'A端入口',
      taskTrip: '500m',
      creator: '管理员',
      executor: '操作员'
    })
    expect(ElMessage.success).toHaveBeenCalledWith('任务保存成功')
    expect(wrapper.emitted('saved')?.[0]?.[0]).toMatchObject({ id: 9 })
    expect(wrapper.emitted('update:modelValue')?.at(-1)?.[0]).toBe(false)
  })

  it('新增任务勾选立即启动后，saved 事件应携带 autoStart 为 true', async () => {
    const wrapper = mountDialog()
    await wrapper.setProps({ modelValue: true })
    await flushPromises()

    await fillRequiredFields(wrapper)
    await wrapper.find('.auto-start').setChecked(true)
    await clickSave(wrapper)

    expect(wrapper.emitted('saved')?.[0]?.[1]).toBe(true)
  })

  it('点击生成按钮应重新获取任务编号', async () => {
    const wrapper = mountDialog()
    await wrapper.setProps({ modelValue: true })
    await flushPromises()

    taskApi.nextCode.mockResolvedValueOnce({ data: 'TASK20260629002' })
    const generateButton = wrapper.findAll('button').find(btn => btn.text().includes('生成'))
    await generateButton.trigger('click')
    await flushPromises()

    expect(taskApi.nextCode).toHaveBeenCalledTimes(2)
    expect(wrapper.find('input[placeholder="自动生成，可手动调整"]').element.value).toBe('TASK20260629002')
  })

  it('新增模式如果已有任务编号，则打开时不应自动调用 nextCode', async () => {
    const wrapper = mountDialog({ task: { taskCode: 'CUSTOM-CODE', taskName: '草稿任务' } })
    await wrapper.setProps({ modelValue: true })
    await flushPromises()

    expect(taskApi.nextCode).not.toHaveBeenCalled()
    expect(wrapper.find('input[placeholder="自动生成，可手动调整"]').element.value).toBe('CUSTOM-CODE')
  })

  it('编辑任务时应携带原任务 id 调用 update 接口', async () => {
    const wrapper = mountDialog({
      mode: 'edit',
      task: { id: 3, taskCode: 'TASK-EDIT', taskName: '旧任务', startPos: 'B口', taskTrip: '200m', creator: 'A', executor: 'B' }
    })
    await wrapper.setProps({ modelValue: true })
    await flushPromises()

    expect(wrapper.text()).toContain('修改巡检任务')
    expect(wrapper.find('.auto-start').exists()).toBe(false)
    await wrapper.find('input[placeholder="如：1号线 K12+300-K12+800 区间巡检"]').setValue('修改后的任务')
    await clickSave(wrapper)

    expect(taskApi.update).toHaveBeenCalledTimes(1)
    expect(taskApi.update.mock.calls[0][0]).toMatchObject({ id: 3, taskCode: 'TASK-EDIT', taskName: '修改后的任务' })
  })

  it('取消按钮应关闭弹窗并触发 update:modelValue', async () => {
    const wrapper = mountDialog()
    await wrapper.setProps({ modelValue: true })
    await flushPromises()

    const cancelButton = wrapper.findAll('button').find(btn => btn.text().includes('取消'))
    await cancelButton.trigger('click')

    expect(wrapper.emitted('update:modelValue')?.at(-1)?.[0]).toBe(false)
  })

  it('获取任务编号失败时应展示错误提示', async () => {
    taskApi.nextCode.mockRejectedValue(new Error('编号接口异常'))
    const wrapper = mountDialog()
    await wrapper.setProps({ modelValue: true })
    await flushPromises()

    expect(ElMessage.error).toHaveBeenCalledWith('编号接口异常')
  })

  it('保存接口失败时应展示错误提示，并恢复保存状态', async () => {
    taskApi.add.mockRejectedValue(new Error('保存失败'))
    const wrapper = mountDialog()
    await wrapper.setProps({ modelValue: true })
    await flushPromises()

    await fillRequiredFields(wrapper)
    await clickSave(wrapper)

    expect(ElMessage.error).toHaveBeenCalledWith('保存失败')
    expect(wrapper.findAll('button').find(btn => btn.text().includes('保存任务')).attributes('data-loading')).toBe('false')
  })
})
