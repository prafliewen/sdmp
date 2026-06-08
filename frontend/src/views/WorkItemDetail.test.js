import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { installMock } from '../../test/helpers.js'
import WorkItemDetail from './WorkItemDetail.vue'

function makeRouter(initialPath) {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'WorkItemList', component: { template: '<div/>' } },
      { path: '/work-items/:id', name: 'WorkItemDetail', component: WorkItemDetail }
    ]
  }).push(initialPath)
}

async function mountDetail(id = 1) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'WorkItemList', component: { template: '<div/>' } },
      { path: '/work-items/:id', name: 'WorkItemDetail', component: WorkItemDetail }
    ]
  })
  await router.push(`/work-items/${id}`)
  await router.isReady()
  const wrapper = mount(WorkItemDetail, {
    global: { plugins: [router] }
  })
  await flushPromises()
  await nextTick()
  await flushPromises()
  return { wrapper, router }
}

describe('WorkItemDetail.vue', () => {
  let mock
  beforeEach(() => {
    setActivePinia(createPinia())
    mock = installMock()
  })
  afterEach(() => mock.restore())

  it('加载完成后渲染工作项标题与状态', async () => {
    mock.onGet('/api/v1/work-items/1').reply(200, {
      code: 0,
      data: { id: 1, code: 'WI-001', title: '登录模块', status: 'DRAFT', type: 'STORY', priority: 'P0' }
    })
    mock.onGet('/api/v1/work-items/1/clarifications').reply(200, { code: 0, data: { records: [], total: 0 } })
    mock.onGet('/api/v1/work-items/1/transitions').reply(200, { code: 0, data: { records: [], total: 0 } })
    mock.onGet('/api/v1/work-items/1/ai-analyses').reply(200, { code: 0, data: { records: [], total: 0 } })

    const { wrapper } = await mountDetail(1)
    expect(wrapper.text()).toContain('登录模块')
    expect(wrapper.text()).toContain('WI-001')
    expect(wrapper.text()).toContain('草稿')
  })

  it('加载失败时展示"工作项不存在"', async () => {
    mock.onGet('/api/v1/work-items/999').reply(200, { code: 500, message: 'fail' })
    mock.onGet('/api/v1/work-items/999/clarifications').reply(200, { code: 0, data: { records: [], total: 0 } })
    mock.onGet('/api/v1/work-items/999/transitions').reply(200, { code: 0, data: { records: [], total: 0 } })
    mock.onGet('/api/v1/work-items/999/ai-analyses').reply(200, { code: 0, data: { records: [], total: 0 } })

    const { wrapper } = await mountDetail(999)
    expect(wrapper.text()).toContain('工作项不存在')
  })

  it('添加澄清问题会触发 POST 并刷新列表', async () => {
    mock.onGet('/api/v1/work-items/1').reply(200, {
      code: 0, data: { id: 1, code: 'WI-001', title: 'T', status: 'DRAFT', type: 'STORY', priority: 'P1' }
    })
    mock.onGet('/api/v1/work-items/1/clarifications').reply(200, { code: 0, data: { records: [], total: 0 } })
    mock.onGet('/api/v1/work-items/1/transitions').reply(200, { code: 0, data: { records: [], total: 0 } })
    mock.onGet('/api/v1/work-items/1/ai-analyses').reply(200, { code: 0, data: { records: [], total: 0 } })
    let postBody
    mock.onPost('/api/v1/work-items/1/clarifications').reply(config => {
      postBody = JSON.parse(config.data)
      return [200, { code: 0, data: { id: 1 } }]
    })

    const { wrapper } = await mountDetail(1)
    await wrapper.findAll('button').find(b => b.text() === '新增澄清问题')?.trigger('click')
    await flushPromises()
    const textarea = wrapper.find('textarea[placeholder*="请详细描述"]')
    await textarea.setValue('问题A')
    await wrapper.findAll('button').find(b => b.text() === '提交')?.trigger('click')
    await flushPromises()
    expect(postBody.question).toBe('问题A')
  })

  it('空标题提交不触发请求', async () => {
    mock.onGet('/api/v1/work-items/1').reply(200, {
      code: 0, data: { id: 1, code: 'WI-001', title: 'T', status: 'DRAFT', type: 'STORY', priority: 'P1' }
    })
    mock.onGet('/api/v1/work-items/1/clarifications').reply(200, { code: 0, data: { records: [], total: 0 } })
    mock.onGet('/api/v1/work-items/1/transitions').reply(200, { code: 0, data: { records: [], total: 0 } })
    mock.onGet('/api/v1/work-items/1/ai-analyses').reply(200, { code: 0, data: { records: [], total: 0 } })

    let posted = false
    mock.onPost('/api/v1/work-items/1/clarifications').reply(() => { posted = true; return [200, { code: 0 }] })

    const { wrapper } = await mountDetail(1)
    await wrapper.findAll('button').find(b => b.text() === '新增澄清问题')?.trigger('click')
    await flushPromises()
    await wrapper.findAll('button').find(b => b.text() === '提交')?.trigger('click')
    await flushPromises()
    expect(posted).toBe(false)
  })
})
