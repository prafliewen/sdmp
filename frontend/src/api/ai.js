import request from '../utils/request'

export function triggerAiAnalysis(workItemId, data) {
  return request({ url: `/api/v1/work-items/${workItemId}/ai-analyses`, method: 'post', data })
}

export function getAiAnalyses(workItemId, params) {
  return request({ url: `/api/v1/work-items/${workItemId}/ai-analyses`, method: 'get', params })
}