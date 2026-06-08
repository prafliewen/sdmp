import request from '../utils/request'

export function getWorkItemList(params) {
  return request({ url: '/api/v1/work-items', method: 'get', params })
}

export function getWorkItemDetail(id) {
  return request({ url: `/api/v1/work-items/${id}`, method: 'get' })
}

export function createWorkItem(data) {
  return request({ url: '/api/v1/work-items', method: 'post', data })
}

export function updateWorkItem(id, data) {
  return request({ url: `/api/v1/work-items/${id}`, method: 'put', data })
}

export function deleteWorkItem(id) {
  return request({ url: `/api/v1/work-items/${id}`, method: 'delete' })
}