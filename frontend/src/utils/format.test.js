import { describe, expect, it } from 'vitest'
import { formatTime, normalizeDistance, percent, safeNumber } from './format'

describe('utils/format', () => {
  it('formatTime 应把后端时间格式转换成页面展示格式，并兼容空值', () => {
    expect(formatTime('2026-06-29T19:30:15')).toBe('2026-06-29 19:30:15')
    expect(formatTime('2026-06-29 19:30:15.000')).toBe('2026-06-29 19:30:15')
    expect(formatTime(null)).toBe('-')
    expect(formatTime('')).toBe('-')
  })

  it('safeNumber 应处理正常数值、默认小数位和非法数值', () => {
    expect(safeNumber(12.345, 2)).toBe('12.35')
    expect(safeNumber('8')).toBe('8.0')
    expect(safeNumber('abc')).toBe('0')
  })

  it('percent 应把当前位置限制在 0 到 100 之间，并兼容空值和总数为 0', () => {
    expect(percent(25, 100)).toBe(25)
    expect(percent(150, 100)).toBe(100)
    expect(percent(-10, 100)).toBe(0)
    expect(percent(undefined, 100)).toBe(0)
    expect(percent(10, 0)).toBe(100)
  })

  it('normalizeDistance 应从任务距离文本中提取数字，并兼容无数字文本', () => {
    expect(normalizeDistance('500m')).toBe(500)
    expect(normalizeDistance('约 12.5 米')).toBe(12.5)
    expect(normalizeDistance('')).toBe(100)
    expect(normalizeDistance('暂无距离')).toBe(100)
  })
})
