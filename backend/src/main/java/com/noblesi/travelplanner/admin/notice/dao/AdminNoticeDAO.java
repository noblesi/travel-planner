package com.noblesi.travelplanner.admin.notice.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.admin.notice.dto.AdminNoticeDetailDTO;
import com.noblesi.travelplanner.admin.notice.dto.AdminNoticeFormDTO;
import com.noblesi.travelplanner.admin.notice.dto.AdminNoticeListDTO;
import com.noblesi.travelplanner.admin.notice.dto.AdminNoticeSearchDTO;

@Mapper
public interface AdminNoticeDAO {
	long countNoticeList(@Param("search") AdminNoticeSearchDTO search);
	List<AdminNoticeListDTO> selectNoticeList(@Param("search") AdminNoticeSearchDTO search);
	AdminNoticeDetailDTO selectNoticeDetail(@Param("noticeId") Long noticeId);
	int insertNotice(@Param("adminId") Long adminId, @Param("form") AdminNoticeFormDTO form);
	int updateNotice(@Param("noticeId") Long noticeId, @Param("form") AdminNoticeFormDTO form);
	int deleteNotice(@Param("noticeId") Long noticeId);
}
