package com.noblesi.travelplanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.domain.member.MemberInfoDomain;
import com.noblesi.travelplanner.dto.member.MemberInfoRequest;
import com.noblesi.travelplanner.dto.member.MemberRewordPassword;
import com.noblesi.travelplanner.dto.member.MemberRewordPasswordRequest;
import com.noblesi.travelplanner.dto.member.NickNameRequest;
import com.noblesi.travelplanner.dto.member.SearchMemberPassword;
import com.noblesi.travelplanner.mapper.MyPageMapper;
import com.noblesi.travelplanner.security.MemberPrincipal;

@Service
public class MyPageService {
    @Autowired(required = false)
    private MyPageMapper myPageMapper;

    private final PasswordEncoder passwordEncoder;

    public MyPageService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
    
    public MemberInfoDomain searchUserInfo(long memberId){
        MemberInfoDomain memberInfoDomain = null;
        memberInfoDomain = myPageMapper.selectMemberInfo(memberId);
        return memberInfoDomain;
    }

    public boolean modifyUserInfo(MemberInfoRequest memberInfoRequest) {
        return myPageMapper.updateMemberInfo(memberInfoRequest) > 0;
    }

    public boolean modifyNickname(long memberId, String nickname){
        NickNameRequest nickNameRequest = new NickNameRequest(memberId,nickname);
        int temp = myPageMapper.updateNickname(nickNameRequest);
        boolean flag = temp > 0;
        return flag;
    }

    public boolean modifyProfileImage(String profileImage){
        //이미지 업로드 경로와 이미지의 이름을 재설정 하여 업데이트 해야한다.
        return myPageMapper.updateProfileImage(profileImage) > 0;
    }

    public boolean modifyPassword(MemberPrincipal principal, MemberRewordPasswordRequest memberRewordPasswordRequest){
       
        boolean flag = false;
        String resultPass = "";

        resultPass = myPageMapper.selectCurrentPassword(principal.memberId());
        System.out.println(resultPass + "검색된 current pass");
       if (passwordEncoder.matches(memberRewordPasswordRequest.currentPassword(), resultPass)) {
           System.out.println("password matches");
            MemberRewordPassword rewordPassword = new MemberRewordPassword(
               principal.memberId(), 
               passwordEncoder.encode(memberRewordPasswordRequest.rewordPassword())
           );
    
           if(myPageMapper.updatePassword(rewordPassword) > 0){
               flag = true;
           } else {
               flag = false;
           }
       }


        System.out.println(flag + "false And True");
        return flag;
    }

    public boolean deleteAccount(int memberId){
        return myPageMapper.updateMemberStatus(memberId) > 0;
    }

}
