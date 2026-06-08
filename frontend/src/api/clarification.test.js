import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { installMock } from '../../test/helpers.js'
import {
  getClarifications,
  addClarification,
  resolveClarification
} from './clarification.js'

describe('api/clarification', () => {
  let mock
  beforeEach(() => {
    setActivePinia(createPinia())
    mock = installMock()
  })
  afterEach(() => mock.restore())

  it('getClarifications 使用 GET 路径', async () => {
    mock.onGet('/api/v1/work-items/1/clarifications').reply(200, { code: 0, data: [] })
    await expect(getClarifications(1, {})).resolves.toBeDefined()
  })

  it('addClarification 使用 POST 路径', async () => {
    const body = { question: 'q', severity: 'P0' }
    mock.onPost('/api/v1/work-items/1/clarifications').reply(config => {
      expect(JSON.parse(config.data)).toEqual(body)
      return [200, { code: 0, data: { id: 9 } }]
    })
    const data = await addClarification(1, body)
    expect(data.id).toBe(9)
  })

  it('resolveClarification 使用 PUT /api/v1/clarifications/:id', async () => {
    const body = { answer: 'a', status: 'RESOLVED' }
    mock.onPut('/api/v1/clarifications/7').reply(config => {
      expect(JSON.parse(config.data)).toEqual(body)
      return [200, { code: 0 }]
    })
    await expect(resolveClarification(7, body)).resolves.toBeUndefined()
  })
})
