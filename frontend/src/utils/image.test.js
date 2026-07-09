import { describe, expect, it } from 'vitest'
import { resolveImageCandidates, resolveImageUrl } from './image'

describe('utils/image', () => {
  it('应保留 http/data/blob 等可直接访问的图片地址', () => {
    expect(resolveImageCandidates('http://example.com/a.jpg')).toEqual(['http://example.com/a.jpg'])
    expect(resolveImageUrl('data:image/png;base64,xxx')).toBe('data:image/png;base64,xxx')
    expect(resolveImageUrl('blob:http://local/demo')).toBe('blob:http://local/demo')
  })

  it('应把 Windows 路径转换成前端 public/images 候选路径', () => {
    const list = resolveImageCandidates('D:\\runs\\crack\\demo_predict.jpg')
    expect(list).toContain('/images/crack/demo_predict.jpg')
    expect(list).toContain('/images/crack/demo.jpg')
    expect(list).toContain('/images/flaw/demo_predict.jpg')
  })

  it('应兼容数据库中已经保存的 /images 路径，并对中文和空格编码', () => {
    const list = resolveImageCandidates('/images/flaw/裂缝 1.jpg')
    expect(list[0]).toBe('/images/flaw/%E8%A3%82%E7%BC%9D%201.jpg')
    expect(list).toContain('/images/flaw/%E8%A3%82%E7%BC%9D%201_predict.jpg')
    expect(list).toContain('/images/crack/%E8%A3%82%E7%BC%9D%201.jpg')
  })

  it('应处理空路径、全空格路径和没有文件名的路径', () => {
    expect(resolveImageCandidates(null)).toEqual([])
    expect(resolveImageCandidates('   ')).toEqual([])
    expect(resolveImageCandidates('/images/')[0]).toBe('/images/')
    expect(resolveImageUrl(null)).toBe('')
  })

  it('应处理没有扩展名的文件名，不重复生成候选路径', () => {
    const list = resolveImageCandidates('/tmp/crack/demo')
    expect(list).toEqual(['/images/crack/demo', '/images/flaw/demo'])
  })

  it('应根据 flaw 路径优先返回 flaw 目录，并兼容 _predict 变体', () => {
    const list = resolveImageCandidates('/server/flaw/demo.jpg')
    expect(list[0]).toBe('/images/flaw/demo.jpg')
    expect(list[1]).toBe('/images/flaw/demo_predict.jpg')
    expect(list).toContain('/images/crack/demo.jpg')
  })
})
