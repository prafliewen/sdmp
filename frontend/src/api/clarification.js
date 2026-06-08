import request from '../utils/request'

export function getClarifications(workItemId, params) {
  return request({ url: `/api/v1/work-items/${workItemId}/clarifications`, method: 'get', params })
}

export function addClarification(workItemId, data) {
  return request({ url: `/api/v1/work-items/${workItemId}/clarifications`, method: 'post', data })
}

export function resolveClarification(id, data) {
  return request({ url: `/api/v1/clarifications/${id}`, method: 'put', data })
}