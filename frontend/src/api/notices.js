import http from './http'

export async function getNoticeList({ category, page = 1, size = 10 } = {}) {
  const response = await http.get('/notices', { params: { category, page, size } })
  return response.data.data
}

export async function getNoticeDetail(noticeId) {
  const encodedId = encodeURIComponent(String(noticeId))
  const response = await http.get(`/notices/${encodedId}`)
  return response.data.data
}
