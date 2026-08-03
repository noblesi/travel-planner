import { beforeEach, describe, expect, it, vi } from 'vitest'

const { httpMock } = vi.hoisted(() => ({
  httpMock: {
    get: vi.fn(),
    interceptors: {
      request: {
        use: vi.fn(),
      },
    },
  },
}))

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => httpMock),
  },
}))

import { attachCsrfToken } from '@/api/http'

beforeEach(() => {
  httpMock.get.mockReset()
})

describe('HTTP CSRF interceptor', () => {
  it('CSRF 처리기를 request interceptor로 등록한다', () => {
    expect(httpMock.interceptors.request.use).toHaveBeenCalledWith(attachCsrfToken)
  })

  it.each(['post', 'put', 'patch', 'delete'])('%s 요청에 CSRF header를 추가한다', async (method) => {
    httpMock.get.mockResolvedValue({
      data: {
        data: { headerName: 'X-CSRF-TOKEN', token: 'csrf-token' },
      },
    })
    const config = { method, headers: { Accept: 'application/json' } }

    await expect(attachCsrfToken(config)).resolves.toEqual({
      method,
      headers: {
        Accept: 'application/json',
        'X-CSRF-TOKEN': 'csrf-token',
      },
    })
    expect(httpMock.get).toHaveBeenCalledWith('/auth/csrf')
  })

  it('GET 요청은 CSRF token을 조회하지 않는다', async () => {
    const config = { method: 'get' }

    await expect(attachCsrfToken(config)).resolves.toBe(config)
    expect(httpMock.get).not.toHaveBeenCalled()
  })

  it('CSRF 응답이 잘못되면 변경 요청을 중단한다', async () => {
    httpMock.get.mockResolvedValue({ data: { data: null } })

    await expect(attachCsrfToken({ method: 'post' })).rejects.toThrow(
      'CSRF token response is invalid',
    )
  })
})
