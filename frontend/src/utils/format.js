export function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

export function safeNumber(value, digits = 1) {
  const n = Number(value)
  if (Number.isNaN(n)) return '0'
  return n.toFixed(digits)
}

export function percent(position, total) {
  const p = Number(position || 0)
  const t = Number(total || 1)
  return Math.max(0, Math.min(100, Math.round((p / t) * 100)))
}

export function normalizeDistance(taskTrip) {
  if (!taskTrip) return 100
  const matched = String(taskTrip).match(/\d+(\.\d+)?/)
  return matched ? Number(matched[0]) : 100
}
