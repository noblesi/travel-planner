import { describe, expect, it } from 'vitest'

import { getSafeAuthenticationRedirect } from '@/utils/authRedirect'

describe('getSafeAuthenticationRedirect', () => {
  it('내부 경로를 유지한다', () => {
    expect(getSafeAuthenticationRedirect('/plans/101/edit')).toBe('/plans/101/edit')
  })

  it.each(['//evil.example', 'https://evil.example', '', null, undefined])(
    '외부 또는 잘못된 경로 %s를 홈으로 바꾼다',
    (value) => {
      expect(getSafeAuthenticationRedirect(value)).toBe('/')
    },
  )
})
