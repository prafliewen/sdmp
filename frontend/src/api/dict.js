import request from '../utils/request'

export function getDicts(type) {
  return request({ url: '/api/v1/dicts', method: 'get', params: { type } })
}