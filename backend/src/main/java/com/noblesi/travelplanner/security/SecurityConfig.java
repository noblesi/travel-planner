package com.noblesi.travelplanner.security;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import com.noblesi.travelplanner.admin.auth.security.AdminAuthenticationProvider;
import com.noblesi.travelplanner.common.api.ErrorResponse;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CsrfTokenRepository csrfTokenRepository() {
		return new HttpSessionCsrfTokenRepository();
	}

	@Bean
	SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository) {
		return new CompositeSessionAuthenticationStrategy(List.of(
				new ChangeSessionIdAuthenticationStrategy(),
				new CsrfAuthenticationStrategy(csrfTokenRepository)
		));
	}

	@Bean
	AuthenticationManager authenticationManager(MemberAuthenticationProvider authenticationProvider) {
		return new ProviderManager(authenticationProvider);
	}



	@Bean
	@Order(1)
	SecurityFilterChain adminSecurityFilterChain(
			HttpSecurity http,
			AdminAuthenticationProvider authenticationProvider,
			ObjectMapper objectMapper
	) throws Exception {
		AuthenticationManager adminAuthenticationManager = new ProviderManager(authenticationProvider);
		AccessDeniedHandlerImpl adminAccessDeniedHandler = new AccessDeniedHandlerImpl();
		adminAccessDeniedHandler.setErrorPage("/admin/error/403");
		http
				.securityMatcher("/admin/**", "/api/admin/**", "/assets/admin/**")
				.authenticationManager(adminAuthenticationManager)
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/admin/login", "/admin/error/**", "/assets/admin/**").permitAll()
						.anyRequest().hasRole("ADMIN"))
				.formLogin(form -> form
						.loginPage("/admin/login")
						.loginProcessingUrl("/admin/login")
						.usernameParameter("loginId")
						.passwordParameter("password")
						.defaultSuccessUrl("/admin/dashboard", true)
						.failureUrl("/admin/login?error"))
				.logout(logout -> logout
						.logoutUrl("/admin/logout")
						.logoutSuccessUrl("/admin/login?logout")
						.invalidateHttpSession(true)
						.deleteCookies("JSESSIONID"))
				.exceptionHandling(exceptions -> exceptions
						.accessDeniedHandler((request, response, exception) -> {
							request.setAttribute("admin.originalRequestUri", request.getRequestURI());
							adminAccessDeniedHandler.handle(request, response, exception);
						})
						.authenticationEntryPoint((request, response, exception) -> {
							if (request.getRequestURI().startsWith(request.getContextPath() + "/api/admin/")) {
								writeError(
										response,
										objectMapper,
										HttpServletResponse.SC_UNAUTHORIZED,
										ErrorResponse.of(
												"ADMIN_LOGIN_REQUIRED",
												"관리자 로그인이 필요합니다.",
												request.getRequestURI()
										)
								);
								return;
							}
							response.sendRedirect(request.getContextPath() + "/admin/login");
						}));
		return http.build();
	}

	@Bean
	@Order(2)
	@ConditionalOnProperty(name = "app.auth.enforce-security", havingValue = "true", matchIfMissing = true)
	SecurityFilterChain securedApiFilterChain(
			HttpSecurity http,
			ObjectMapper objectMapper,
			CsrfTokenRepository csrfTokenRepository,
			MemberSessionValidationFilter memberSessionValidationFilter
	) throws Exception {
		http
				.csrf(csrf -> csrf
						.csrfTokenRepository(csrfTokenRepository))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(
								"/api/auth/**",
								"/api/account-recovery/**",
								"/api/health",
								"/api/health/live",
								"/error").permitAll()
						.requestMatchers("/uploads/profile/**").permitAll()
						.requestMatchers("/api/admin/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/members/me").authenticated()
						.requestMatchers(HttpMethod.PATCH, "/api/members/me").authenticated()
						.requestMatchers(HttpMethod.PATCH, "/api/members/me/password").authenticated()
						.requestMatchers(HttpMethod.PATCH, "/api/members/me/profile-image").authenticated()
						.requestMatchers(HttpMethod.DELETE, "/api/members/me").authenticated()
						.requestMatchers(HttpMethod.OPTIONS, "/api/users/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/users/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/users/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/regions").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/places/search").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/plans/mine").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/plans", "/api/plans/*").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/notices", "/api/notices/*").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/plan-invitations/**").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(memberSessionValidationFilter, AuthorizationFilter.class)
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) -> writeError(
								response,
								objectMapper,
								HttpServletResponse.SC_UNAUTHORIZED,
								ErrorResponse.of(
										"CURRENT_MEMBER_NOT_AVAILABLE",
										"로그인이 필요합니다.",
										request.getRequestURI()
								)
						))
						.accessDeniedHandler((request, response, exception) -> {
							boolean csrfFailure = exception instanceof CsrfException;
							writeError(
									response,
									objectMapper,
									HttpServletResponse.SC_FORBIDDEN,
									ErrorResponse.of(
											csrfFailure ? "CSRF_TOKEN_INVALID" : "ACCESS_DENIED",
											csrfFailure
													? "CSRF token이 없거나 만료되었습니다."
													: "요청을 수행할 권한이 없습니다.",
											request.getRequestURI()
									)
							);
						}));
		return http.build();
	}

	@Bean
	@Order(2)
	@ConditionalOnProperty(name = "app.auth.enforce-security", havingValue = "false")
	SecurityFilterChain localDevelopmentFilterChain(
			HttpSecurity http,
			CsrfTokenRepository csrfTokenRepository,
			MemberSessionValidationFilter memberSessionValidationFilter
	) throws Exception {
			http
				.csrf(csrf -> csrf
						.csrfTokenRepository(csrfTokenRepository)
						.ignoringRequestMatchers(
								// local profile의 mutation API에는 개발용 CSRF 예외 정책을 적용한다.
								"/api/plans/**",
								"/api/plan-invitations/**"))
				.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
				.addFilterBefore(memberSessionValidationFilter, AuthorizationFilter.class);
		return http.build();
	}

	private void writeError(
			HttpServletResponse response,
			ObjectMapper objectMapper,
			int status,
			ErrorResponse errorResponse
	) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), errorResponse);
	}
}
