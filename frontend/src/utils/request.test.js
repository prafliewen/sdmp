import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { installMock } from '../../test/helpers.js'
import request from './request.js'

describe('utils/request', () => {
  let mock

  beforeEach(() => {
    mock = installMock()
  })

  afterEach(() => {
    mock.restore()
  })

  it('请求成功且 code===0 时 resolve 解包后的 data', async () => {
    mock.onGet('/api/v1/work-items').reply(200, {
      code: 0,
      message: 'success',
      data: { records: [], total: 0 }
    })

    const data = await request({ url: '/api/v1/work-items', method: 'get' })
    expect(data.records).toBeDefined()
    expect(data.total).toBe(0)
  })

  it('请求成功但 code !== 0 时 reject 并附带后端 message', async () => {
    mock.onGet('/api/v1/work-items').reply(200, {
      code: 500,
      message: '服务端错误'
    })

    await expect(request({ url: '/api/v1/work-items', method: 'get' }))
      .rejects.toThrow('服务端错误')
  })

  it('HTTP 网络错误时 reject', async () => {
    mock.onGet('/api/v1/work-items').networkError()

    await expect(request({ url: '/api/v1/work-items', method: 'get' }))
      .rejects.toBeDefined()
  })

  it('HTTP 5xx 时 reject（响应拦截器对后端 message 透传）', async () => {
    mock.onGet('/api/v1/work-items').reply(500, {
      code: 500,
      message: '内部异常'
    })

    await expect(request({ url: '/api/v1/work-items', method: 'get' }))
      .rejects.toBeDefined()
  })

  it('Content-Type 默认为 application/json', async () => {
    let captured
    mock.onGet('/api/v1/me').reply(config => {
      captured = config.headers
      return [200, { code: 0, message: 'ok' }]
    })
    await request({ url: '/api/v1/me', method: 'get' })
    expect(captured['Content-Type']).toContain('application/json')
  })

  it('当存在 operator 时附加 X-User header', async () => {
    setActivePinia(createPinia())
    const { useUserStore } = await import('../store/user.js')
    const store = useUserStore()
    store.operator = 'alice'
    let captured
    mock.onGet('/api/v1/me').reply(config => {
      captured = config.headers
      return [200, { code: 0, message: 'ok' }]
    })
    await request({ url: '/api/v1/me', method: 'get' })
    expect(captured['X-User']).toBe('alice')
  })
})
