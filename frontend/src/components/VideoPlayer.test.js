import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import VideoPlayer from './VideoPlayer.vue'

const stubs = {
  'el-alert': { props: ['title'], template: '<div class="alert">{{ title }}</div>' }
}

describe('VideoPlayer', () => {
  beforeEach(() => {
    delete window.EasyPlayerPro
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('没有播放地址时应提示未获取到视频播放地址', async () => {
    const wrapper = mount(VideoPlayer, { props: { url: '', cameraName: '前方视角' }, global: { stubs } })
    await flushPromises()
    expect(wrapper.text()).toContain('未获取到视频播放地址')
    expect(wrapper.text()).toContain('前方视角')
    expect(wrapper.text()).toContain('暂无播放地址')
  })

  it('有地址但未加载播放器脚本时应提示检查 EasyPlayer', async () => {
    const wrapper = mount(VideoPlayer, { props: { url: 'http://demo/live/cam1.flv' }, global: { stubs } })
    await flushPromises()
    expect(wrapper.text()).toContain('未加载 EasyPlayer-pro.js')
  })

  it('加载 EasyPlayer 后应创建播放器并调用 play', async () => {
    const play = vi.fn()
    const destroy = vi.fn()
    window.EasyPlayerPro = vi.fn(function () { return { play, destroy } })
    const wrapper = mount(VideoPlayer, { props: { url: 'http://demo/live/cam1.flv' }, global: { stubs } })
    await flushPromises()
    expect(window.EasyPlayerPro).toHaveBeenCalled()
    expect(play).toHaveBeenCalledWith('http://demo/live/cam1.flv')
    expect(window.EasyPlayerPro.mock.calls[0][1]).toMatchObject({ hasAudio: false, isMute: true })
    wrapper.unmount()
    expect(destroy).toHaveBeenCalled()
  })

  it('muted 为 false 时应启用音频配置', async () => {
    const play = vi.fn()
    window.EasyPlayerPro = vi.fn(function () { return { play, destroy: vi.fn() } })
    mount(VideoPlayer, { props: { url: 'http://demo/live/cam1.flv', muted: false }, global: { stubs } })
    await flushPromises()

    expect(window.EasyPlayerPro.mock.calls[0][1]).toMatchObject({ hasAudio: true, isMute: false })
  })

  it('播放器对象没有 play 方法时应展示对应错误', async () => {
    window.EasyPlayerPro = vi.fn(function () { return { destroy: vi.fn() } })
    const wrapper = mount(VideoPlayer, { props: { url: 'http://demo/live/cam1.flv' }, global: { stubs } })
    await flushPromises()

    expect(wrapper.text()).toContain('EasyPlayerPro 已加载，但没有找到 play 方法')
  })

  it('播放器初始化抛出异常时应展示初始化失败原因', async () => {
    window.EasyPlayerPro = vi.fn(function () { throw new Error('初始化崩溃') })
    const wrapper = mount(VideoPlayer, { props: { url: 'http://demo/live/cam1.flv' }, global: { stubs } })
    await flushPromises()

    expect(wrapper.text()).toContain('EasyPlayer 初始化失败：初始化崩溃')
  })

  it('播放地址变化时应销毁旧播放器并重新播放新地址', async () => {
    const firstDestroy = vi.fn()
    const secondDestroy = vi.fn()
    const play = vi.fn()
    window.EasyPlayerPro = vi
      .fn()
      .mockImplementationOnce(function () { return { play, destroy: firstDestroy } })
      .mockImplementationOnce(function () { return { play, destroy: secondDestroy } })

    const wrapper = mount(VideoPlayer, { props: { url: 'http://demo/live/cam1.flv' }, global: { stubs } })
    await flushPromises()
    await wrapper.setProps({ url: 'http://demo/live/cam2.flv' })
    await flushPromises()

    expect(firstDestroy).toHaveBeenCalled()
    expect(play).toHaveBeenLastCalledWith('http://demo/live/cam2.flv')
  })
})
