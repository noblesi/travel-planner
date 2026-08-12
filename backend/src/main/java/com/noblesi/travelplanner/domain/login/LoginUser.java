package com.noblesi.travelplanner.domain.login;

import java.sql.Date;


import org.apache.ibatis.type.Alias;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString

@Alias("loginUser")
public class LoginUser {

    private Long id;
    private String email;
    private String name;
    private String profileImageUrl;
    private String nickname;
    private String gender;
    private Date birth;
    private String createdAt;
    private String withdrawnAt;

}