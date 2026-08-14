import { afterEach, describe, expect, it, vi } from 'vitest'

import { readLocalStorage, writeLocalStorage } from '@/utils/browserStorage'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('browserStorage', () => {
  it('returns a fallback when localStorage reads are blocked', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('Storage is blocked', 'SecurityError')
    })

    expect(readLocalStorage('blocked-key', 'fallback')).toBe('fallback')
  })

  it('reports a failed write instead of propagating the storage error', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('Storage quota exceeded', 'QuotaExceededError')
    })

    expect(writeLocalStorage('blocked-key', 'value')).toBe(false)
  })
})
