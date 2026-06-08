import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { installMock } from '../../test/helpers.js'
import { getDicts } from './dict.js'

describe('api/dict', () => {
  let mock
  beforeEach(() => {
    setActivePinia(createPinia())
    mock = installMock()
  })
  afterEach(() => mock.restore())

  it('getDicts 传 type 作为 query 参数', async () => {
    mock.onGet('/api/v1/dicts').reply(config => {
      expect(config.params).toEqual({ type: 'PRIORITY' })
      return [200, { code: 0, data: [{ code: 'P0', label: 'P0' }] }]
    })
    const data = await getDicts('PRIORITY')
    expect(data[0].code).toBe('P0')
  })
})
