package com.noblesi.travelplanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.domain.member.MemberInfoDomain;
import com.noblesi.travelplanner.dto.member.MemberInfoRequest;
import com.noblesi.travelplanner.mapper.MyPageMapper;

@Service
public class MyPageService {
    @Autowired(required = false)
    private MyPageMapper myPageMapper;
    
    public MemberInfoDomain searchUserInfo(long memberId){
        MemberInfoDomain memberInfoDomain = null;
        System.out.println("emember service =================="+memberId + " // " + myPageMapper);
        memberInfoDomain = myPageMapper.selectMemberInfo(memberId);
        return memberInfoDomain;
    }

    public boolean modifyUserInfo(MemberInfoRequest memberInfoRequest) {
        return myPageMapper.updateMemberInfo(memberInfoRequest) > 0;
    }

    public boolean modifyNickname(String nickname){
        return myPageMapper.updateNickname(nickname) > 0;
    }

    public boolean modifyProfileImage(String profileImage){
        //이미지 업로드 경로와 이미지의 이름을 재설정 하여 업데이트 해야한다.
        return myPageMapper.updateProfileImage(profileImage) > 0;
    }

    public boolean deleteAccount(int memberId){
        return myPageMapper.updateMemberStatus(memberId) > 0;
    }

}
