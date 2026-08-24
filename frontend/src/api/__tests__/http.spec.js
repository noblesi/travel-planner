import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'

const { httpMock } = vi.hoisted(() => ({
  httpMock: {
    get: vi.fn(),
    request: vi.fn(),
    interceptors: {
      request: {
        use: vi.fn(),
      },
      response: {
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

import {
  attachCsrfToken,
  clearCsrfTokenCache,
  recoverExpiredCsrfToken,
} from '@/api/http'

beforeEach(() => {
  httpMock.get.mockReset()
  httpMock.request.mockReset()
  // module scope cache가 테스트 간 요청 횟수 검증에 영향을 주지 않도록 각 case를 독립시킨다.
  clearCsrfTokenCache()
})

describe('HTTP CSRF interceptor', () => {
  it('allows the backend external API timeout budget to finish', () => {
    expect(axios.create).toHaveBeenCalledWith(
      expect.objectContaining({
        timeout: 12000,
      }),
    )
  })

  it('CSRF 처리기를 request interceptor로 등록한다', () => {
    expect(httpMock.interceptors.request.use).toHaveBeenCalledWith(attachCsrfToken)
  })

  it('CSRF 복구 처리기를 response interceptor로 등록한다', () => {
    expect(httpMock.interceptors.response.use).toHaveBeenCalledWith(
      undefined,
      recoverExpiredCsrfToken,
    )
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

  it('연속 mutation은 검증된 CSRF token을 재사용한다', async () => {
    httpMock.get.mockResolvedValue({
      data: { data: { headerName: 'X-CSRF-TOKEN', token: 'cached-token' } },
    })

    await attachCsrfToken({ method: 'post' })
    await attachCsrfToken({ method: 'delete' })

    expect(httpMock.get).toHaveBeenCalledTimes(1)
  })

  it('동시에 시작한 mutation은 하나의 CSRF 조회 요청을 공유한다', async () => {
    let resolveCsrf
    httpMock.get.mockReturnValue(
      new Promise((resolve) => {
        resolveCsrf = resolve
      }),
    )

    const first = attachCsrfToken({ method: 'post' })
    const second = attachCsrfToken({ method: 'patch' })
    resolveCsrf({ data: { data: { headerName: 'X-CSRF-TOKEN', token: 'shared-token' } } })

    await Promise.all([first, second])
    expect(httpMock.get).toHaveBeenCalledTimes(1)
  })

  it('cache 초기화 전에 시작된 늦은 CSRF 응답을 다시 저장하지 않는다', async () => {
    let resolveOldCsrf
    httpMock.get
      .mockReturnValueOnce(
        new Promise((resolve) => {
          resolveOldCsrf = resolve
        }),
      )
      .mockResolvedValueOnce({
        data: { data: { headerName: 'X-CSRF-TOKEN', token: 'new-session-token' } },
      })

    const oldRequest = attachCsrfToken({ method: 'post' })
    clearCsrfTokenCache()
    const newRequest = attachCsrfToken({ method: 'post' })
    await newRequest

    resolveOldCsrf({ data: { data: { headerName: 'X-CSRF-TOKEN', token: 'old-session-token' } } })
    await oldRequest

    const nextConfig = await attachCsrfToken({ method: 'delete' })
    expect(nextConfig.headers['X-CSRF-TOKEN']).toBe('new-session-token')
    expect(httpMock.get).toHaveBeenCalledTimes(2)
  })

  it('CSRF 응답이 잘못되면 변경 요청을 중단한다', async () => {
    httpMock.get.mockResolvedValue({ data: { data: null } })

    await expect(attachCsrfToken({ method: 'post' })).rejects.toThrow(
      'CSRF token response is invalid',
    )
  })

  it('만료된 CSRF token이면 cache를 비우고 원 요청을 한 번 재시도한다', async () => {
    const retriedResponse = { data: { success: true } }
    httpMock.request.mockResolvedValue(retriedResponse)
    const error = {
      config: { method: 'post', url: '/plans', data: { title: '여행' } },
      response: { status: 403, data: { code: 'CSRF_TOKEN_INVALID' } },
    }

    await expect(recoverExpiredCsrfToken(error)).resolves.toBe(retriedResponse)
    expect(httpMock.request).toHaveBeenCalledWith({
      method: 'post',
      url: '/plans',
      data: { title: '여행' },
      _csrfRetried: true,
    })
  })

  it('이미 재시도한 CSRF 실패는 다시 요청하지 않는다', async () => {
    const error = {
      config: { method: 'post', url: '/plans', _csrfRetried: true },
      response: { status: 403, data: { code: 'CSRF_TOKEN_INVALID' } },
    }

    await expect(recoverExpiredCsrfToken(error)).rejects.toBe(error)
    expect(httpMock.request).not.toHaveBeenCalled()
  })

  it('일반 권한 오류는 CSRF 재시도 대상으로 처리하지 않는다', async () => {
    const error = {
      config: { method: 'post', url: '/plans' },
      response: { status: 403, data: { code: 'ACCESS_DENIED' } },
    }

    await expect(recoverExpiredCsrfToken(error)).rejects.toBe(error)
    expect(httpMock.request).not.toHaveBeenCalled()
  })
})
