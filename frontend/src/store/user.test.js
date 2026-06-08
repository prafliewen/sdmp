import { setActivePinia, createPinia } from 'pinia'
import { describe, it, expect, beforeEach } from 'vitest'
import { useUserStore } from '../store/user.js'

describe('user store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('operator 默认值为 candidate', () => {
    const store = useUserStore()
    expect(store.operator).toBe('candidate')
  })

  it('operator 允许修改', () => {
    const store = useUserStore()
    store.operator = 'alice'
    expect(store.operator).toBe('alice')
  })

  it('多次调用返回同一实例', () => {
    const a = useUserStore()
    const b = useUserStore()
    expect(a).toBe(b)
  })
})
