import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import StatCard from './StatCard.vue'

function mountCard(props) {
  return mount(StatCard, {
    props,
    global: {
      stubs: {
        'el-icon': { template: '<span><slot /></span>' },
        Warning: { template: '<i />' },
        Monitor: { template: '<i />' }
      }
    }
  })
}

describe('StatCard', () => {
  it('应正确展示统计名称、数值和说明文字', () => {
    const wrapper = mountCard({ label: '故障总数', value: 3, desc: '待复核 1 条', icon: 'Warning', type: 'orange' })
    expect(wrapper.text()).toContain('故障总数')
    expect(wrapper.text()).toContain('3')
    expect(wrapper.text()).toContain('待复核 1 条')
    expect(wrapper.find('.stat-icon').classes()).toContain('orange')
  })

  it('未传 desc 时不应渲染说明文字区域，并使用默认图标和类型', () => {
    const wrapper = mountCard({ label: '在线设备', value: 2 })
    expect(wrapper.text()).toContain('在线设备')
    expect(wrapper.text()).toContain('2')
    expect(wrapper.find('.stat-desc').exists()).toBe(false)
    expect(wrapper.find('.stat-icon').classes()).toContain('blue')
  })
})
