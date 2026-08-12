package com.noblesi.travelplanner.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.dto.member.JoinMemberRequest;
import com.noblesi.travelplanner.mapper.MemberMapper;

@Service
public class MemberJoinService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    public MemberJoinService(MemberMapper memberMapper, PasswordEncoder passwordEncoder){
        this.memberMapper = memberMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /*
     * 이메일 중복 체크
     * @param email
     * @return true : 이미 존재하는 이메일, false : 사용 가능한 이메일 
     */
    public boolean searchEmail(String email){
        int searchCnt = memberMapper.selectEmailCnt(email);
        System.out.println("email : " + email + " searchCnt : " + searchCnt);
        return searchCnt > 0;
    }

    /*
     * 유저 데이터를 받아서 mapper class로 보내준다.
     * @param joinMemberRequest
     * @return true : 회원가입 성공, false : 회원가입 실패
     */
    public boolean addMember(JoinMemberRequest joinMemberRequest){
        boolean insertFlag = false;

        JoinMemberRequest joinMemberRequestHash = new JoinMemberRequest(
            joinMemberRequest.email(),
            passwordEncoder.encode(joinMemberRequest.password()),
            joinMemberRequest.name(),
            joinMemberRequest.gender(),
            joinMemberRequest.birth(),
            joinMemberRequest.privacy(),
            joinMemberRequest.phone()
        );
        
        int insertCnt = memberMapper.insertMember(joinMemberRequestHash);
        
        if( insertCnt > 0 ){
            insertFlag = true;
        }

        return insertFlag;
    }
    
}
