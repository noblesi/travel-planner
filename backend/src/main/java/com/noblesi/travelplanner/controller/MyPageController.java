package com.noblesi.travelplanner.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/member")
public class MyPageController {
    
    @Autowired(required = false)
    private MyPageService myPageService;

    @GetMapping("/myPage")
    public ApiResponse<MemberInfoDomain> getSearchMemberInfo(Authentication authentication){
        try {
            MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
			return ApiResponse.success( myPageService.searchUserInfo(principal.memberId()) );
		} catch (BadCredentialsException exception) {
			throw new BusinessException(
					HttpStatus.UNAUTHORIZED,
					"INVALID_LOGIN_CREDENTIALS",
					"이메일 또는 비밀번호가 올바르지 않습니다."
			);
		}
        //return ApiResponse.success(myPageService.searchUserInfo(memberId));
    }

    @GetMapping("/modifyMemberInfo")
    public ApiResponse<Boolean> postModifyMemberInfo(@RequestParam MemberInfoRequest memberInfoRequest) {
        return ApiResponse.success(myPageService.modifyUserInfo(memberInfoRequest));
    }

    @GetMapping("/modifyNickname")
    public ApiResponse<Boolean> postModifyNickname(@RequestBody String nickname) {
        return ApiResponse.success(myPageService.modifyNickname(nickname));
    }
    
    @PostMapping("/modifyProfileImage")
    public ApiResponse<Boolean> postModifyProfileImage(@RequestBody String profileImage) {
        return ApiResponse.success(myPageService.modifyProfileImage(profileImage));
    }

    @GetMapping("/deleteAccount")
    public ApiResponse<Boolean> postDeleteAccount(@RequestBody int memberId) {
        return ApiResponse.success(myPageService.deleteAccount(memberId));
    }
    
}
