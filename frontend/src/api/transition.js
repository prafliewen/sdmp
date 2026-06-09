import request from '../utils/request'

export function transitWorkItem(id, data) {
  return request({ url: `/api/v1/work-items/${id}/transitions`, method: 'post', data })
}

export function getTransitionHistory(id, params) {
  return request({ url: `/api/v1/work-items/${id}/transitions`, method: 'get', params })
}

export function getAllowedTransitions(id) {
  return request({ url: `/api/v1/work-items/${id}/transitions/allowed`, method: 'get' })
}