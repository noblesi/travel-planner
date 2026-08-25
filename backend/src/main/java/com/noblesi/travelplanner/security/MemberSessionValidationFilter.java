package com.noblesi.travelplanner.security;

import java.io.IOException;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.noblesi.travelplanner.domain.member.AuthenticatedMember;
import com.noblesi.travelplanner.mapper.MemberMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberSessionValidationFilter extends OncePerRequestFilter {

	private final ObjectProvider<MemberMapper> memberMapperProvider;
	private final SecurityContextRepository securityContextRepository =
			new HttpSessionSecurityContextRepository();
	private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String apiPathPrefix = request.getContextPath() + "/api/";
		return !request.getRequestURI().startsWith(apiPathPrefix) || request.getSession(false) == null;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| !(authentication.getPrincipal() instanceof MemberPrincipal principal)) {
			filterChain.doFilter(request, response);
			return;
		}

		MemberMapper memberMapper = memberMapperProvider.getIfAvailable();
		if (memberMapper == null) {
			filterChain.doFilter(request, response);
			return;
		}

		AuthenticatedMember member = memberMapper.findForSessionValidation(principal.memberId());
		if (member == null || !member.isActive()) {
			logoutHandler.logout(request, response, authentication);
			filterChain.doFilter(request, response);
			return;
		}

		if (!Objects.equals(principal.email(), member.email())
				|| !Objects.equals(principal.displayName(), member.displayName())) {
			refreshPrincipal(request, response, authentication, member);
		}

		filterChain.doFilter(request, response);
	}

	private void refreshPrincipal(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication,
			AuthenticatedMember member
	) {
		MemberPrincipal principal = new MemberPrincipal(
				member.memberId(),
				member.email(),
				member.displayName()
		);
		UsernamePasswordAuthenticationToken refreshed = UsernamePasswordAuthenticationToken.authenticated(
				principal,
				authentication.getCredentials(),
				authentication.getAuthorities()
		);
		refreshed.setDetails(authentication.getDetails());

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(refreshed);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
	}
}
