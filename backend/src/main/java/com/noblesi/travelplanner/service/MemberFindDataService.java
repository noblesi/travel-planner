package com.noblesi.travelplanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.dto.login.EmailFindRequest;
import com.noblesi.travelplanner.dto.login.MemberRewordPasswordRequest;
import com.noblesi.travelplanner.dto.login.PasswordFindRewordRequest;
import com.noblesi.travelplanner.mapper.MemberFindDataMapper;

@Service
public class MemberFindDataService {
    @Autowired(required = false)
    private MemberFindDataMapper memberFindDataMapper;

    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

    public String searchEmailFind(EmailFindRequest emailFindRequest){
        return memberFindDataMapper.selectEmailFind(emailFindRequest);
    }

    public long searchRewordPassword(PasswordFindRewordRequest passwordFindRewordRequest){
        return memberFindDataMapper.selectPasswordFind(passwordFindRewordRequest);
    }

    public boolean rewordPassword(MemberRewordPasswordRequest memberRewordPasswordRequest){

        MemberRewordPasswordRequest tempMemberRewordPasswordRequest = 
        new MemberRewordPasswordRequest(
            memberRewordPasswordRequest.email(),
            passwordEncoder.encode(memberRewordPasswordRequest.newPassword())
        );

        return memberFindDataMapper.updateRewordPassword(tempMemberRewordPasswordRequest)>0;
    }


}
