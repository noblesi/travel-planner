import http from './http'

export async function searchPlaces({ keyword, regionCode, category, page = 1, size = 10 }) {
  const params = {
    keyword: keyword.trim(),
    page,
    size,
  }

  if (regionCode) {
    params.regionCode = regionCode
  }
  if (category) {
    params.category = category
  }

  const response = await http.get('/places/search', { params })
  return response.data.data
}
