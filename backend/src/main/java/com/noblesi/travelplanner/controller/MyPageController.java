package com.noblesi.travelplanner.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.member.MemberInfoDomain;
import com.noblesi.travelplanner.dto.member.MemberInfoRequest;

import com.noblesi.travelplanner.dto.member.MemberRewordPasswordRequest;
import com.noblesi.travelplanner.security.MemberPrincipal;
import com.noblesi.travelplanner.service.MyPageService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
					"DATA_LEADING_FAIL",
					"데이터를 불러오는데 실패하였습니다."
			);
		}
        //return ApiResponse.success(myPageService.searchUserInfo(memberId));
    }

    @PostMapping("/modifyMemberInfo")
    public ApiResponse<Boolean> postModifyMemberInfo(Authentication authentication, @RequestBody MemberInfoRequest memberInfoRequest) {
       try{
            MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
            MemberInfoRequest changeMemberInfoRequest = new MemberInfoRequest(
                    principal.memberId(),            
                    memberInfoRequest.memberName(),
                    memberInfoRequest.email(),
                    memberInfoRequest.genderCode(),
                    memberInfoRequest.phoneNumber(),
                    memberInfoRequest.birthDate()
            );
           return ApiResponse.success(myPageService.modifyUserInfo(changeMemberInfoRequest));
        } catch (BadCredentialsException exception) {
			throw new BusinessException(
					HttpStatus.UNAUTHORIZED,
					"MEMBER_INFO_UPDATE_FAIL",
					"데이터를 업데이트에 실패 하였습니다."
			);
        }
    }

    @GetMapping("/modifyNickname")
    public ApiResponse<Boolean> getModifyNickname(Authentication authentication, @RequestParam String nickname) {
        try {
            MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
			return ApiResponse.success( myPageService.modifyNickname(principal.memberId(),nickname) );
		} catch (BadCredentialsException exception) {
			throw new BusinessException(
					HttpStatus.UNAUTHORIZED,
					"NICKNAME_UPDATE_FAIL",
					"닉네임 변경을 실패하였습니다."
			);
		}
    }
    @PostMapping("/modifyProfileImage")
    public ApiResponse<Boolean> postModifyProfileImage(Authentication authentication, @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(myPageService.modifyProfileImage(authentication, file));
    }

    @PostMapping("/modifyPassword")
    public ApiResponse<Boolean> postModifyPassword(Authentication authentication, @RequestBody MemberRewordPasswordRequest memberRewordPasswordRequest) {
        System.out.println("passwordReword!!!!!");
        try {
            MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
			return ApiResponse.success(myPageService.modifyPassword(principal, memberRewordPasswordRequest));
		} catch (BadCredentialsException exception) {
			throw new BusinessException(
					HttpStatus.UNAUTHORIZED,
					"PASSWORD_REWORD_FAIL",
					"비밀번호 변경을 실패하였습니다."
			);
		}
    }
    
    @GetMapping("/deleteAccount")
    public ApiResponse<Boolean> getDeleteAccount(Authentication authentication) {
        return ApiResponse.success(myPageService.deleteAccount(authentication));
    }
    
}
