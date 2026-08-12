package com.noblesi.travelplanner.service;

import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.domain.login.LoginUser;
import com.noblesi.travelplanner.mapper.LoginMapper;

@Service
public class LoginService {
    
    private final LoginMapper loginMapper;

    public LoginService(LoginMapper loginMapper) {
        this.loginMapper = loginMapper;
    }

    public LoginUser login(String email, String password) {
        return loginMapper.selectLogin(email, password);
    }


}
