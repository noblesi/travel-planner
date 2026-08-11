package com.noblesi.travelplanner.service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
class SmtpInvitationMailSender implements InvitationMailSender {

	private static final Logger log = LoggerFactory.getLogger(SmtpInvitationMailSender.class);
	private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private final JavaMailSender javaMailSender;
	private final String fromAddress;

	SmtpInvitationMailSender(
			JavaMailSender javaMailSender,
			@Value("${app.mail.from}") String fromAddress
	) {
		this.javaMailSender = javaMailSender;
		this.fromAddress = fromAddress;
	}

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
