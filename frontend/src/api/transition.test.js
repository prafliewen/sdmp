import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { installMock } from '../../test/helpers.js'
import { transitWorkItem, getTransitionHistory } from './transition.js'

describe('api/transition', () => {
  let mock
  beforeEach(() => {
    setActivePinia(createPinia())
    mock = installMock()
  })
  afterEach(() => mock.restore())

  it('transitWorkItem 使用 POST /:id/transitions', async () => {
    const body = { targetStatus: 'READY' }
    mock.onPost('/api/v1/work-items/1/transitions').reply(config => {
      expect(JSON.parse(config.data)).toEqual(body)
      return [200, { code: 0, data: { from: 'DRAFT', to: 'READY' } }]
    })
    const data = await transitWorkItem(1, body)
    expect(data.to).toBe('READY')
  })

  it('getTransitionHistory 使用 GET /:id/transitions', async () => {
    mock.onGet('/api/v1/work-items/1/transitions').reply(200, { code: 0, data: [] })
    await expect(getTransitionHistory(1, {})).resolves.toBeDefined()
  })
})
