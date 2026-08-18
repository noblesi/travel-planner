package com.noblesi.travelplanner.dto.member;

import java.time.LocalDate;
import java.util.Locale;

import org.apache.ibatis.type.Alias;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Alias("joinMemberRequest")
public record JoinMemberRequest(
        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 255, message = "이메일은 255자 이하로 입력해 주세요.")
        String email,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 10, max = 72, message = "비밀번호는 10자 이상 72자 이하로 입력해 주세요.")
        String password,

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 10, message = "이름은 10자 이하로 입력해 주세요.")
        String name,

        @Size(max = 50, message = "닉네임은 50자 이하로 입력해 주세요.")
        String nickname,

        @NotBlank(message = "성별을 선택해 주세요.")
        @Pattern(regexp = "M|F|N", message = "성별 코드가 올바르지 않습니다.")
        String gender,

        @NotNull(message = "생년월일을 입력해 주세요.")
        @JsonFormat(pattern = "yyyyMMdd")
        LocalDate birth,

        @NotBlank(message = "개인정보 저장에 동의해 주세요.")
        @Pattern(regexp = "Y", message = "개인정보 저장 동의가 필요합니다.")
        String privacy,

        @NotBlank(message = "전화번호를 입력해 주세요.")
        @Size(max = 20, message = "전화번호는 20자 이하로 입력해 주세요.")
        @Pattern(regexp = "\\d{2,3}-?\\d{3,4}-?\\d{4}", message = "전화번호 형식이 올바르지 않습니다.")
        String phone
) {
    public JoinMemberRequest {
        if (email != null) {
            email = email.strip().toLowerCase(Locale.ROOT);
        }
        if (name != null) {
            name = name.strip();
        }
        if (nickname != null) {
            nickname = nickname.strip();
        }
        if (nickname == null || nickname.isBlank()) {
            nickname = name;
        }
        if (gender != null) {
            gender = gender.strip().toUpperCase(Locale.ROOT);
        }
        if (privacy != null) {
            privacy = privacy.strip().toUpperCase(Locale.ROOT);
        }
        if (phone != null) {
            phone = phone.strip();
        }
    }
    public String getEmail() {
        return email;
    }
}
