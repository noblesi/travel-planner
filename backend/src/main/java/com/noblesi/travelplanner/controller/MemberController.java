package com.noblesi.travelplanner.controller;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.dto.member.JoinMemberRequest;
import com.noblesi.travelplanner.service.MemberJoinService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class MemberController {

    private final MemberJoinService memberJoinService;

    public MemberController(MemberJoinService memberJoinService) {
        this.memberJoinService = memberJoinService;
    }

    @PostMapping("/join")
    public ApiResponse<Boolean> joinMember(@Valid @RequestBody JoinMemberRequest userInfo) {
        return ApiResponse.success(memberJoinService.addMember(userInfo));
    }

    @GetMapping("/emailCheck")
    public ApiResponse<Boolean> getMemberEmailCheck(@RequestParam("email") String email) {
        return ApiResponse.success(memberJoinService.searchEmail(email));
    }
}
