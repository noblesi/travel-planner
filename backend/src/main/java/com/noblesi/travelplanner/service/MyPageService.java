package com.noblesi.travelplanner.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.noblesi.travelplanner.domain.member.MemberInfoDomain;
import com.noblesi.travelplanner.dto.member.MemberInfoRequest;
import com.noblesi.travelplanner.dto.member.MemberProfileChangeRequest;
import com.noblesi.travelplanner.dto.member.MemberRewordPassword;
import com.noblesi.travelplanner.dto.member.MemberRewordPasswordRequest;
import com.noblesi.travelplanner.dto.member.NickNameRequest;
import com.noblesi.travelplanner.mapper.MyPageMapper;
import com.noblesi.travelplanner.security.MemberPrincipal;

@Service
public class MyPageService {
    @Autowired(required = false)
    private MyPageMapper myPageMapper;

    private final PasswordEncoder passwordEncoder;

    @Value("${app.profile.upload-path}")
    private String uploadDir;

    @Value("${app.backend.base-url}")
    private String backendUrl;

    public MyPageService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
    
    public MemberInfoDomain searchUserInfo(long memberId){
        MemberInfoDomain memberInfoDomain = null;
        memberInfoDomain = new MemberInfoDomain(
            myPageMapper.selectMemberInfo(memberId).memberId(),
            myPageMapper.selectMemberInfo(memberId).memberName(),
            myPageMapper.selectMemberInfo(memberId).email(),
            myPageMapper.selectMemberInfo(memberId).nickname(),
            myPageMapper.selectMemberInfo(memberId).genderCode(),
            myPageMapper.selectMemberInfo(memberId).phoneNumber(),
            myPageMapper.selectMemberInfo(memberId).profileImageUrl(),
            myPageMapper.selectMemberInfo(memberId).memberStatus(),
            myPageMapper.selectMemberInfo(memberId).createdAt(),
            myPageMapper.selectMemberInfo(memberId).withdrawnAt(),
            myPageMapper.selectMemberInfo(memberId).birthDate()
        );
        return memberInfoDomain;
    }

    public boolean modifyUserInfo(MemberInfoRequest memberInfoRequest) {
        return myPageMapper.updateMemberInfo(memberInfoRequest) > 0;
    }

    public boolean modifyNickname(long memberId, String nickname){
        NickNameRequest nickNameRequest = new NickNameRequest(memberId,nickname);
        int temp = myPageMapper.updateNickname(nickNameRequest);
        boolean flag = temp > 0;
        return flag;
    }

    public boolean modifyProfileImage(Authentication authentication, MultipartFile file){
        try {
            MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
            long memberId = principal.memberId();

            if (file.isEmpty()) {
                throw new IllegalArgumentException("업로드된 파일이 없습니다.");
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String saveFileName = uuid + extension;

            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File destinationFile = new File(uploadDir,saveFileName);
            file.transferTo(destinationFile.getAbsoluteFile());
           
            String profileImageUrl = saveFileName;
            MemberProfileChangeRequest memberProfileChangeRequest = new MemberProfileChangeRequest(memberId, profileImageUrl);

            return myPageMapper.updateProfileImage(memberProfileChangeRequest) > 0;

        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    public boolean modifyPassword(MemberPrincipal principal, MemberRewordPasswordRequest memberRewordPasswordRequest){
       
        boolean flag = false;
        String resultPass = "";

        resultPass = myPageMapper.selectCurrentPassword(principal.memberId());
       if (passwordEncoder.matches(memberRewordPasswordRequest.currentPassword(), resultPass)) {
            MemberRewordPassword rewordPassword = new MemberRewordPassword(
               principal.memberId(), 
               passwordEncoder.encode(memberRewordPasswordRequest.rewordPassword())
           );
    
           if(myPageMapper.updatePassword(rewordPassword) > 0){
               flag = true;
           } else {
               flag = false;
           }
       }


        return flag;
    }

    public boolean deleteAccount(Authentication authentication){
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return myPageMapper.updateMemberStatus(principal.memberId()) > 0;
    }

}
