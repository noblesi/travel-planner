package com.noblesi.travelplanner.service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.config.AsyncConfig;
import com.noblesi.travelplanner.config.MailProperties;

@Component
@Profile("!local")
class SmtpInvitationMailSender implements InvitationMailSender {

	private static final Logger log = LoggerFactory.getLogger(SmtpInvitationMailSender.class);
	private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private final JavaMailSender javaMailSender;
	private final String fromAddress;

	SmtpInvitationMailSender(
			JavaMailSender javaMailSender,
			MailProperties mailProperties
	) {
		this.javaMailSender = javaMailSender;
		this.fromAddress = mailProperties.from();
	}

	// 초대 대상이 여러 명이면 SMTP 왕복이 그만큼 직렬로 누적돼 HTTP 응답(axios 5초 타임아웃)을
	// 넘기기 쉽다. 메일 발송은 이미 실패해도 무시되는 best-effort라 응답을 기다릴 이유가 없으므로 비동기로 던진다.
	@Async(AsyncConfig.INVITATION_MAIL_EXECUTOR)
	@Override
	public void send(String toEmail, String planTitle, String acceptLink, OffsetDateTime expiresAt) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(fromAddress);
		message.setTo(toEmail);
		message.setSubject("[WithTrip] \"" + planTitle + "\" 여행 플랜에 초대되었습니다");
		message.setText(
				"WithTrip에서 \"" + planTitle + "\" 여행 플랜에 초대했습니다.\n\n"
						+ "아래 링크를 눌러 초대를 수락해 주세요:\n" + acceptLink + "\n\n"
						+ "이 링크는 " + EXPIRY_FORMAT.format(expiresAt) + "(UTC)까지 유효합니다."
		);
		try {
			javaMailSender.send(message);
		} catch (MailException exception) {
			log.warn("초대 메일 발송에 실패했습니다. to={}", toEmail, exception);
		}
	}
}
