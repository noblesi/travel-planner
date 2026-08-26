package com.noblesi.travelplanner.controller;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.dto.member.JoinMemberRequest;
import com.noblesi.travelplanner.service.MemberJoinService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class MemberController {

    private final MemberJoinService memberJoinService;

    public MemberController(MemberJoinService memberJoinService) {
        this.memberJoinService = memberJoinService;
    }

    @PostMapping("/join")
    public ApiResponse<Boolean> postJoinMember(@RequestBody JoinMemberRequest userInfo) {
        return ApiResponse.success(memberJoinService.addMember(userInfo));
    }

    @GetMapping("/emailCheck")
    public ApiResponse<Boolean> getMemberEmailCheck(@RequestParam String email) {
        return ApiResponse.success(memberJoinService.searchEmail(email));
    }
}
