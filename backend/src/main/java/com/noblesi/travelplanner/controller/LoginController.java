package com.noblesi.travelplanner.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.domain.login.LoginUser;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/login")
public class LoginController {
    
    
    @PostMapping("/login")
    public ApiResponse<LoginUser> postLogin(@RequestBody LoginUser loginUser) {
        
        
        return ApiResponse.<LoginUser>success(loginUser);
    }
    

}
