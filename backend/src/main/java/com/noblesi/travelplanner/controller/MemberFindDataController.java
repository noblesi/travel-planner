package com.noblesi.travelplanner.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.dto.login.EmailFindRequest;
import com.noblesi.travelplanner.dto.login.MemberRewordPasswordRequest;
import com.noblesi.travelplanner.dto.login.PasswordFindRewordRequest;
import com.noblesi.travelplanner.service.MemberFindDataService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/find")
public class MemberFindDataController {
    @Autowired(required = false)
    MemberFindDataService memberFindDataService;

    @PostMapping("/emailFind")
    public ApiResponse<String> postSearchEmail(@RequestBody EmailFindRequest emailFindRequest){
        System.out.println("Controller in" + emailFindRequest);
        return ApiResponse.success(memberFindDataService.searchEmailFind(emailFindRequest));
    }

    @PostMapping("/passwordFind")
    public ApiResponse<Long> postSearchPassword(@RequestBody PasswordFindRewordRequest passwordFindRewordRequest){
        System.out.println("passwordfind : " + passwordFindRewordRequest);
        return ApiResponse.success(memberFindDataService.searchRewordPassword(passwordFindRewordRequest));
    }

    @PostMapping("/passwordReword")
    public ApiResponse<Boolean> postPasswordReword(@RequestBody MemberRewordPasswordRequest memberRewordPasswordRequest) {
        return ApiResponse.success(memberFindDataService.rewordPassword(memberRewordPasswordRequest));
    }
    
    
    
}
