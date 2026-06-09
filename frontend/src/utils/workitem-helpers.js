export const statusLabelMap = {
  DRAFT: '草稿',
  ANALYZING: '分析中',
  READY: '就绪',
  IN_PROGRESS: '进行中',
  IN_TESTING: '测试中',
  DONE: '已完成'
}

export function statusLabel(status) {
  return statusLabelMap[status] || status
}

export function formatDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return '-'
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

export function getTypeTagClass(type) {
  if (type === 'STORY') return 'story'
  if (type === 'BUG') return 'bug'
  if (type === 'TASK') return 'task'
  return ''
}

export function getPriorityTagClass(priority) {
  if (!priority) return ''
  const digit = priority.replace('P', '').toLowerCase()
  return `p${digit}`
}
