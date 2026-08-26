package com.noblesi.travelplanner.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.noblesi.travelplanner.domain.member.MemberInfoDomain;
import com.noblesi.travelplanner.dto.member.MemberInfoRequest;
import com.noblesi.travelplanner.dto.member.MemberProfileChangeRequest;
import com.noblesi.travelplanner.dto.member.MemberRewordPassword;
import com.noblesi.travelplanner.dto.member.NickNameRequest;

@Mapper
public interface MyPageMapper {
    
    public MemberInfoDomain selectMemberInfo(long memberId);

    public int updateMemberInfo(MemberInfoRequest memberInfoRequest);

    public int updateNickname(NickNameRequest nickNameRequest);

    public int updateProfileImage(MemberProfileChangeRequest memberProfileChangeRequest);

    public int updatePassword(MemberRewordPassword memberRewordPassword);

    public String selectCurrentPassword(long memberId);

    public int updateMemberStatus(long memberId);

}
