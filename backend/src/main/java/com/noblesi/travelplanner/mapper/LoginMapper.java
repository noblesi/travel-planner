package com.noblesi.travelplanner.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.noblesi.travelplanner.domain.login.LoginUser;

@Mapper
public interface LoginMapper {

    public LoginUser selectLogin(String email, String password);
    
}
