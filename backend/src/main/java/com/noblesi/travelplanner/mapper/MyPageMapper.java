package com.noblesi.travelplanner.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.noblesi.travelplanner.domain.member.MemberInfoDomain;
import com.noblesi.travelplanner.dto.member.MemberInfoRequest;

@Mapper
public interface MyPageMapper {
    
    public MemberInfoDomain selectMemberInfo(long memberId);

    public int updateMemberInfo(MemberInfoRequest memberInfoRequest);

    public int updateNickname(String nickname);

    public int updateProfileImage(String profileImage);

    public int updateMemberStatus(int memberId);

}
