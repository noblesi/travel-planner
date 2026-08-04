// 공지사항 카테고리 코드 → 화면에 보여줄 한글 라벨.
// 카테고리는 안내/점검 두 가지로 고정한다.
export const NOTICE_CATEGORY_LABELS = {
  GUIDE: '안내',
  MAINTENANCE: '점검',
}

// 백엔드가 내려주는 ISO 날짜(yyyy-MM-dd)를 화면 표시용(yyyy.MM.dd)으로 바꾼다.
export function formatNoticeDate(isoDate) {
  if (!isoDate) return ''
  return isoDate.replaceAll('-', '.')
}
