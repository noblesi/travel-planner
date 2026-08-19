package com.noblesi.travelplanner.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.member.MemberInfoDomain;
import com.noblesi.travelplanner.dto.auth.AuthenticatedMemberResponse;
import com.noblesi.travelplanner.dto.auth.AuthenticationSessionResponse;
import com.noblesi.travelplanner.dto.member.MemberInfoRequest;
import com.noblesi.travelplanner.security.MemberPrincipal;
import com.noblesi.travelplanner.service.MyPageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/Member")
public class MyPageController {
    
    @Autowired(required = false)
    private MyPageService myPageService;

    @PostMapping("/myPage")
    public ApiResponse<MemberInfoDomain> postSearchMemberInfo(@RequestBody int memberId){
        System.out.println("postSearchMemberInfo method in");
        return ApiResponse.success(myPageService.searchUserInfo(memberId));
    }

    @PostMapping("/modifyMemberInfo")
    public ApiResponse<Boolean> postModifyMemberInfo(@RequestParam MemberInfoRequest memberInfoRequest) {
        return ApiResponse.success(myPageService.modifyUserInfo(memberInfoRequest));
    }

    @PostMapping("/modifyNickname")
    public ApiResponse<Boolean> postModifyNickname(@RequestBody String nickname) {
        return ApiResponse.success(myPageService.modifyNickname(nickname));
    }
    
    @PostMapping("/modifyProfileImage")
    public ApiResponse<Boolean> postModifyProfileImage(@RequestBody String profileImage) {
        return ApiResponse.success(myPageService.modifyProfileImage(profileImage));
    }

    @PostMapping("/deleteAccount")
    public ApiResponse<Boolean> postDeleteAccount(@RequestBody int memberId) {
        return ApiResponse.success(myPageService.deleteAccount(memberId));
    }
    
}
