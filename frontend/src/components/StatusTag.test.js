import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusTag from './StatusTag.vue'

function mountTag(value) {
  return mount(StatusTag, {
    props: { value },
    global: {
      stubs: {
        'el-tag': {
          props: ['type'],
          template: '<span class="status-tag" :data-type="type"><slot /></span>'
        }
      }
    }
  })
}

describe('StatusTag', () => {
  it('已完成等成功状态应显示 success 类型', () => {
    const wrapper = mountTag('已完成')
    expect(wrapper.text()).toContain('已完成')
    expect(wrapper.find('.status-tag').attributes('data-type')).toBe('success')
    expect(mountTag('正常').find('.status-tag').attributes('data-type')).toBe('success')
  })

  it('巡视中、运行中等执行状态应显示 primary 类型', () => {
    expect(mountTag('巡视中').find('.status-tag').attributes('data-type')).toBe('primary')
    expect(mountTag('报警中').find('.status-tag').attributes('data-type')).toBe('primary')
  })

  it('待巡视、疑似、低中风险等状态应显示 warning 类型', () => {
    expect(mountTag('待巡视').find('.status-tag').attributes('data-type')).toBe('warning')
    expect(mountTag('疑似').find('.status-tag').attributes('data-type')).toBe('warning')
    expect(mountTag('中').find('.status-tag').attributes('data-type')).toBe('warning')
  })

  it('高、异常、误报等状态应显示 danger 类型', () => {
    expect(mountTag('高').find('.status-tag').attributes('data-type')).toBe('danger')
    expect(mountTag('误报').find('.status-tag').attributes('data-type')).toBe('danger')
  })

  it('未知状态应显示 info 类型并兼容空值', () => {
    expect(mountTag('').text()).toContain('-')
    expect(mountTag('其他').find('.status-tag').attributes('data-type')).toBe('info')
  })
})
