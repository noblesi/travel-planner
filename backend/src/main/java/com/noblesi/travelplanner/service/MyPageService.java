package com.noblesi.travelplanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.domain.member.MemberInfoDomain;
import com.noblesi.travelplanner.dto.member.MemberInfoRequest;
import com.noblesi.travelplanner.dto.member.NickNameRequest;
import com.noblesi.travelplanner.mapper.MyPageMapper;

@Service
public class MyPageService {
    @Autowired(required = false)
    private MyPageMapper myPageMapper;
    
    public MemberInfoDomain searchUserInfo(long memberId){
        MemberInfoDomain memberInfoDomain = null;
        memberInfoDomain = myPageMapper.selectMemberInfo(memberId);
        System.out.println("memeberinfodoamin : " + memberInfoDomain);
        return memberInfoDomain;
    }

    public boolean modifyUserInfo(MemberInfoRequest memberInfoRequest) {
        return myPageMapper.updateMemberInfo(memberInfoRequest) > 0;
    }

    public boolean modifyNickname(long memberId, String nickname){
        NickNameRequest nickNameRequest = new NickNameRequest(memberId,nickname);
        System.out.println(nickNameRequest.toString());
        int temp = myPageMapper.updateNickname(nickNameRequest);
        System.out.println(temp+"값이 나오냐?");
        boolean flag = temp > 0;
        return flag;
    }

    public boolean modifyProfileImage(String profileImage){
        //이미지 업로드 경로와 이미지의 이름을 재설정 하여 업데이트 해야한다.
        return myPageMapper.updateProfileImage(profileImage) > 0;
    }

    public boolean deleteAccount(int memberId){
        return myPageMapper.updateMemberStatus(memberId) > 0;
    }

}
