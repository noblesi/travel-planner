package com.noblesi.travelplanner.security;

import java.util.List;
import java.util.Locale;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.domain.member.AuthenticatedMember;
import com.noblesi.travelplanner.mapper.MemberMapper;

@Component
public class MemberAuthenticationProvider implements AuthenticationProvider {

	private final MemberMapper memberMapper;
	private final PasswordEncoder passwordEncoder;

	public MemberAuthenticationProvider(
			MemberMapper memberMapper,
			PasswordEncoder passwordEncoder
	) {
		this.memberMapper = memberMapper;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String email = normalizeEmail(authentication.getName());
		String password = String.valueOf(authentication.getCredentials());
		AuthenticatedMember member = memberMapper.findForEmailAuthentication(email);

		if (member == null
				|| !member.isActive()
				|| !member.hasLocalCredential()
				|| !passwordEncoder.matches(password, member.passwordHash())) {
			throw new BadCredentialsException("Invalid email or password");
		}

		MemberPrincipal principal = new MemberPrincipal(
				member.memberId(),
				member.email(),
				member.displayName()
		);
		return UsernamePasswordAuthenticationToken.authenticated(
				principal,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
		);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}

	private String normalizeEmail(String email) {
		return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
	}
}
