package com.noblesi.travelplanner.service;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.dto.member.JoinMemberRequest;
import com.noblesi.travelplanner.mapper.MemberMapper;

@Service
public class MemberJoinService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    public MemberJoinService(MemberMapper memberMapper, PasswordEncoder passwordEncoder) {
        this.memberMapper = memberMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /*
     * 이메일 중복 체크
     * @param email
     * @return true : 이미 존재하는 이메일, false : 사용 가능한 이메일 
     */
    @Transactional(readOnly = true)
    public boolean searchEmail(String email) {
        return memberMapper.selectEmailCnt(normalizeEmail(email)) > 0;
    }

    /*
     * 유저 데이터를 받아서 mapper class로 보내준다.
     * @param joinMemberRequest
     * @return true : 회원가입 성공, false : 회원가입 실패
     */
    @Transactional
    public boolean addMember(JoinMemberRequest joinMemberRequest) {
        if (memberMapper.selectEmailCnt(joinMemberRequest.email()) > 0) {
            throw duplicateEmail();
        }

        JoinMemberRequest joinMemberRequestHash = new JoinMemberRequest(
            joinMemberRequest.email(),
            passwordEncoder.encode(joinMemberRequest.password()),
            joinMemberRequest.name(),
            joinMemberRequest.gender(),
            joinMemberRequest.birth(),
            joinMemberRequest.privacy(),
            joinMemberRequest.phone()
        );
        
        try {
            if (memberMapper.insertMember(joinMemberRequestHash) != 1) {
                throw new BusinessException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "MEMBER_JOIN_FAILED",
                        "회원가입에 실패했습니다."
                );
            }
        } catch (DuplicateKeyException exception) {
            throw duplicateEmail();
        }

        return true;
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > 255 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_MEMBER_EMAIL",
                    "올바른 이메일을 입력해 주세요."
            );
        }
        return normalized;
    }

    private BusinessException duplicateEmail() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "DUPLICATE_MEMBER_EMAIL",
                "이미 사용 중인 이메일입니다."
        );
    }
}
