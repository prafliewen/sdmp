import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { installMock } from '../../test/helpers.js'
import {
  getWorkItemList,
  getWorkItemDetail,
  createWorkItem,
  updateWorkItem,
  deleteWorkItem
} from './workitem.js'

describe('api/workitem', () => {
  let mock
  beforeEach(() => {
    setActivePinia(createPinia())
    mock = installMock()
  })
  afterEach(() => mock.restore())

  it('getWorkItemList 使用 GET /api/v1/work-items 传参', async () => {
    mock.onGet('/api/v1/work-items').reply(config => {
      expect(config.method).toBe('get')
      expect(config.params).toEqual({ pageNo: 1, pageSize: 10, keyword: 'k' })
      return [200, { code: 0, data: { records: [], total: 0 } }]
    })
    const data = await getWorkItemList({ pageNo: 1, pageSize: 10, keyword: 'k' })
    expect(data.total).toBe(0)
  })

  it('getWorkItemDetail 使用 GET /:id', async () => {
    mock.onGet('/api/v1/work-items/42').reply(200, { code: 0, data: { id: 42 } })
    const data = await getWorkItemDetail(42)
    expect(data.id).toBe(42)
  })

  it('createWorkItem 使用 POST /', async () => {
    const payload = { title: 't', type: 'STORY', priority: 'P1' }
    mock.onPost('/api/v1/work-items').reply(config => {
      expect(JSON.parse(config.data)).toEqual(payload)
      return [200, { code: 0, data: { id: 1, ...payload } }]
    })
    const data = await createWorkItem(payload)
    expect(data.id).toBe(1)
  })

  it('updateWorkItem 使用 PUT /:id', async () => {
    const payload = { title: 't2', version: 1 }
    mock.onPut('/api/v1/work-items/3').reply(config => {
      expect(JSON.parse(config.data)).toEqual(payload)
      return [200, { code: 0, data: { id: 3 } }]
    })
    await updateWorkItem(3, payload)
  })

  it('deleteWorkItem 使用 DELETE /:id', async () => {
    mock.onDelete('/api/v1/work-items/5').reply(200, { code: 0 })
    await expect(deleteWorkItem(5)).resolves.toBeUndefined()
  })
})
