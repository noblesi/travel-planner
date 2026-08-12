package com.noblesi.travelplanner.admin.auth.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.noblesi.travelplanner.admin.auth.interceptor.AdminSessionInterceptor;

@Configuration
public class AdminWebConfig implements WebMvcConfigurer {

	@Autowired
	private AdminSessionInterceptor adminSessionInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(adminSessionInterceptor)
				.addPathPatterns("/admin/**", "/api/admin/**")
				.excludePathPatterns(
						"/admin/login",
						"/assets/admin/**"
				);
	}
}
