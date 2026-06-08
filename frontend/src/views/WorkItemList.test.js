import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { installMock } from '../../test/helpers.js'
import WorkItemList from './WorkItemList.vue'

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'WorkItemList', component: WorkItemList },
      { path: '/work-items/:id', name: 'WorkItemDetail', component: { template: '<div/>' } }
    ]
  })
}

async function mountList() {
  const router = makeRouter()
  router.push('/')
  await router.isReady()
  const wrapper = mount(WorkItemList, {
    global: { plugins: [router] }
  })
  // onMounted 的 fetchList 是 async，需多等一轮
  await flushPromises()
  await nextTick()
  return { wrapper, router }
}

describe('WorkItemList.vue', () => {
  let mock
  beforeEach(() => {
    setActivePinia(createPinia())
    mock = installMock()
  })
  afterEach(() => mock.restore())

  it('挂载时自动请求列表', async () => {
    mock.onGet('/api/v1/work-items').reply(200, {
      code: 0,
      data: { records: [{ id: 1, code: 'WI-001', title: 'A', type: 'STORY', priority: 'P0', status: 'DRAFT', createdAt: '2026-06-08T10:00:00' }], total: 1 }
    })
    const { wrapper } = await mountList()
    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(1)
    expect(rows[0].text()).toContain('WI-001')
    expect(rows[0].text()).toContain('A')
  })

  it('空数据展示空状态', async () => {
    mock.onGet('/api/v1/work-items').reply(200, { code: 0, data: { records: [], total: 0 } })
    const { wrapper } = await mountList()
    expect(wrapper.find('.empty-state').exists()).toBe(true)
  })

  it('关键字 + 类型 + 优先级 + 状态会作为查询参数', async () => {
    let captured
    mock.onGet('/api/v1/work-items').reply(config => {
      captured = config.params
      return [200, { code: 0, data: { records: [], total: 0 } }]
    })
    const { wrapper } = await mountList()
    await wrapper.find('input[placeholder*="搜索"]').setValue('登录')
    await wrapper.findAll('select')[0].setValue('BUG')
    await wrapper.findAll('select')[1].setValue('P0')
    await wrapper.findAll('select')[2].setValue('DRAFT')
    await wrapper.findAll('button').find(b => b.text() === '搜索')?.trigger('click')
    await flushPromises()
    expect(captured.keyword).toBe('登录')
    expect(captured.type).toBe('BUG')
    expect(captured.priority).toBe('P0')
    expect(captured.status).toBe('DRAFT')
  })

  it('重置按钮清空过滤项并重新拉取', async () => {
    mock.onGet('/api/v1/work-items').reply(200, { code: 0, data: { records: [], total: 0 } })
    const { wrapper } = await mountList()
    const keywordInput = wrapper.find('input[placeholder*="搜索"]')
    await keywordInput.setValue('abc')
    await wrapper.findAll('button').find(b => b.text() === '重置')?.trigger('click')
    await flushPromises()
    expect(keywordInput.element.value).toBe('')
  })

  it('打开创建模态框后填写字段可提交', async () => {
    mock.onGet('/api/v1/work-items').reply(200, { code: 0, data: { records: [], total: 0 } })
    let postBody
    mock.onPost('/api/v1/work-items').reply(config => {
      postBody = JSON.parse(config.data)
      return [200, { code: 0, data: { id: 99, ...postBody } }]
    })
    const { wrapper } = await mountList()
    await wrapper.findAll('button').find(b => b.text() === '创建工作项')?.trigger('click')
    await flushPromises()
    expect(wrapper.find('.modal').exists()).toBe(true)
    await wrapper.find('.modal input').setValue('新工作项')
    // 第二个 select 是类型
    const selects = wrapper.find('.modal').findAll('select')
    await selects[0].setValue('TASK')
    await wrapper.findAll('.modal-actions button').find(b => b.text() === '确认创建')?.trigger('click')
    await flushPromises()
    expect(postBody.title).toBe('新工作项')
    expect(postBody.type).toBe('TASK')
  })

  it('创建时若标题为空则不提交', async () => {
    mock.onGet('/api/v1/work-items').reply(200, { code: 0, data: { records: [], total: 0 } })
    const onPost = vi.fn()
    mock.onPost('/api/v1/work-items').reply(() => {
      onPost()
      return [200, { code: 0, data: {} }]
    })
    const { wrapper } = await mountList()
    await wrapper.findAll('button').find(b => b.text() === '创建工作项')?.trigger('click')
    await flushPromises()
    await wrapper.findAll('.modal-actions button').find(b => b.text() === '确认创建')?.trigger('click')
    await flushPromises()
    expect(onPost).not.toHaveBeenCalled()
  })

  it('分页：下一页按钮触发分页参数变更', async () => {
    mock.onGet('/api/v1/work-items').reply(200, { code: 0, data: { records: [], total: 25 } })
    const { wrapper } = await mountList()
    const next = wrapper.findAll('button').find(b => b.text() === '下一页')
    expect(next).toBeDefined()
    let captured
    mock.onGet('/api/v1/work-items').reply(config => {
      captured = config.params
      return [200, { code: 0, data: { records: [], total: 25 } }]
    })
    next.trigger('click')
    await flushPromises()
    expect(captured.pageNo).toBe(2)
  })
})
