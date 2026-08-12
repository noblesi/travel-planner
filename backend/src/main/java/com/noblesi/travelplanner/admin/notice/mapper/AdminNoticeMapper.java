package com.noblesi.travelplanner.admin.notice.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.admin.notice.dto.AdminNoticeDetailDTO;
import com.noblesi.travelplanner.admin.notice.dto.AdminNoticeFormDTO;
import com.noblesi.travelplanner.admin.notice.dto.AdminNoticeListDTO;
import com.noblesi.travelplanner.admin.notice.dto.AdminNoticeSearchDTO;

@Mapper
public interface AdminNoticeMapper {
	/** 검색 조건에 해당하는 공지사항의 전체 개수를 조회합니다. */
	long countNoticeList(@Param("search") AdminNoticeSearchDTO search);

	/** 검색 조건과 페이지 범위에 해당하는 공지사항 목록을 조회합니다. */
	List<AdminNoticeListDTO> selectNoticeList(@Param("search") AdminNoticeSearchDTO search);

	/** 공지사항 번호에 해당하는 상세 정보를 조회합니다. */
	AdminNoticeDetailDTO selectNoticeDetail(@Param("noticeId") Long noticeId);

	/** 관리자 번호와 입력값을 사용하여 공지사항을 등록합니다. */
	int insertNotice(@Param("adminId") Long adminId, @Param("form") AdminNoticeFormDTO form);

	/** 공지사항 번호에 해당하는 제목, 내용, 분류를 수정합니다. */
	int updateNotice(@Param("noticeId") Long noticeId, @Param("form") AdminNoticeFormDTO form);

	/** 공지사항 번호에 해당하는 데이터를 실제 삭제합니다. */
	int deleteNotice(@Param("noticeId") Long noticeId);
}
