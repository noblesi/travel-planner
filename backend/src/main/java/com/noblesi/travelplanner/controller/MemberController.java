package com.noblesi.travelplanner.controller;

import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.dto.member.JoinMemberRequest;
import com.noblesi.travelplanner.service.MemberJoinService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;



@RestController
@RequestMapping("/users")
public class MemberController {

    private final MemberJoinService memberJoinService;

    public MemberController (MemberJoinService memberJoinService){
        this.memberJoinService = memberJoinService;
    }

   @PostMapping("/join")
    public ApiResponse<Integer> joinMember(@RequestBody JoinMemberRequest userInfo) {
        return ApiResponse.success(memberJoinService.addMember(userInfo));
    }
    
}
