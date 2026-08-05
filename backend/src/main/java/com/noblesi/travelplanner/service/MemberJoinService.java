package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.dto.member.JoinMemberRequest;
import com.noblesi.travelplanner.mapper.MemberMapper;

@Service
public class MemberJoinService {

    private final MemberMapper memberMapper;

    private MemberJoinService(MemberMapper memberMapper){
        this.memberMapper = memberMapper;
    }

    /*
     * 유저 데이터를 받아서 mapper class로 보내준다.
     * @param joinMemberRequest
     * @return
     */
    public int addMember(JoinMemberRequest joinMemberRequest){
        int insertCnt = 0;
        insertCnt = memberMapper.insertMember(joinMemberRequest);
        return insertCnt;
    }
    
}
