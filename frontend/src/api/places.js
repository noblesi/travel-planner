import http from './http'

export async function searchPlaces({ keyword, regionCode, page = 1, size = 10 }) {
  const params = {
    keyword: keyword.trim(),
    page,
    size,
  }

  if (regionCode) {
    params.regionCode = regionCode
  }

  const response = await http.get('/places/search', { params })
  return response.data.data
}
