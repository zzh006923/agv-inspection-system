/**
 * 图片路径统一处理。
 * 目标：后端数据库里无论保存 /images/crack/xxx_predict.jpg、/images/flaw/xxx.jpg、
 * Windows 本地路径、Linux 路径，前端都尽量转换为可访问的 public 静态资源路径。
 *
 * 注意：浏览器不能直接读取 C:\Users\... 或 D:\... 这样的本机路径。
 * 真正要显示的图片文件必须放在前端 public/images/crack 或 public/images/flaw 下，
 * 或者放在后端 static 目录并通过 http 地址暴露出来。
 */
function encodePath(path) {
  return path
    .split('/')
    .map((part, index) => index === 0 ? part : encodeURIComponent(part))
    .join('/')
}

function pushUnique(list, value) {
  if (!value) return
  if (!list.includes(value)) list.push(value)
}

function filenameVariants(fileName) {
  const names = []
  pushUnique(names, fileName)

  const match = fileName.match(/^(.*?)(\.[^.]+)$/)
  if (!match) return names

  const stem = match[1]
  const ext = match[2]

  // 兼容脚本保存到数据库的 xxx_predict.jpg，实际 runs 目录里可能是 xxx.jpg
  if (stem.endsWith('_predict')) {
    pushUnique(names, `${stem.replace(/_predict$/, '')}${ext}`)
  } else {
    pushUnique(names, `${stem}_predict${ext}`)
  }

  return names
}

export function resolveImageCandidates(rawUrl) {
  if (!rawUrl) return []
  const raw = String(rawUrl).trim()
  if (!raw) return []

  const candidates = []

  if (/^(https?:|data:|blob:)/i.test(raw)) {
    pushUnique(candidates, raw)
    return candidates
  }

  const normalized = raw.replace(/\\/g, '/')

  // 如果数据库已经存成 /images/xxx，优先按原路径访问
  if (normalized.startsWith('/images/')) {
    pushUnique(candidates, encodePath(normalized))
  }

  const fileName = normalized.split('/').filter(Boolean).pop()
  if (!fileName) return candidates

  const decodedFileName = decodeURIComponent(fileName)
  const variants = filenameVariants(decodedFileName)

  // 根据原始路径判断优先目录；裂缝识别脚本使用 /images/crack，演示数据可能使用 /images/flaw
  const folders = normalized.includes('/crack/')
    ? ['crack', 'flaw']
    : normalized.includes('/flaw/')
      ? ['flaw', 'crack']
      : ['crack', 'flaw']

  for (const folder of folders) {
    for (const name of variants) {
      pushUnique(candidates, `/images/${folder}/${encodeURIComponent(name)}`)
    }
  }

  return candidates
}

export function resolveImageUrl(rawUrl) {
  return resolveImageCandidates(rawUrl)[0] || ''
}
