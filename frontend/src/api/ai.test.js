import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { installMock } from '../../test/helpers.js'
import { triggerAiAnalysis, getAiAnalyses } from './ai.js'

describe('api/ai', () => {
  let mock
  beforeEach(() => {
    setActivePinia(createPinia())
    mock = installMock()
  })
  afterEach(() => mock.restore())

  it('triggerAiAnalysis 使用 POST 路径', async () => {
    const body = { type: 'SUMMARY' }
    mock.onPost('/api/v1/work-items/2/ai-analyses').reply(config => {
      expect(JSON.parse(config.data)).toEqual(body)
      return [200, { code: 0, data: { id: 11 } }]
    })
    const data = await triggerAiAnalysis(2, body)
    expect(data.id).toBe(11)
  })

  it('getAiAnalyses 使用 GET 路径', async () => {
    mock.onGet('/api/v1/work-items/2/ai-analyses').reply(200, { code: 0, data: [] })
    await expect(getAiAnalyses(2, {})).resolves.toBeDefined()
  })
})
