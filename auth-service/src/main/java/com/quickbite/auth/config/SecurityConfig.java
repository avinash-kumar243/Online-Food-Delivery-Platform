package com.quickbite.auth.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.quickbite.auth.service.OAuth2AuthenticationFailureHandler;
import com.quickbite.auth.service.OAuth2AuthenticationSuccessHandler;

import lombok.RequiredArgsConstructor;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor 
public class SecurityConfig {  
	
/*	This class is the main Spring Security setup class.
	
	It tells our application:

		which URLs are public
		which URLs need JWT token
		how to handle unauthorized access
		where to run our JWTFilter
		how CORS should work
		OAuth2 can create a short-lived session when needed
*/
	
	
	private final JwtFilter filter;  
	private final JWTAuthenticationEntryPoint point; 
	private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
	private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
	private final AppRoleAwareOAuth2AuthorizationRequestResolver appRoleAwareOAuth2AuthorizationRequestResolver;

	@Value("${frontend-url:https://main.d38xhvu2bosgry.amplifyapp.com}")
	private String frontendUrl;

	@Value("${local-frontend-url:http://localhost:4200}")
	private String localFrontendUrl;
	
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
		 	.csrf(csrf -> csrf.disable())
			.cors(Customizer.withDefaults())
			.authorizeHttpRequests(authz -> authz
					.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
					.requestMatchers("/auth/**", "/oauth2/**", "/login/**", "/error", "/h2-console/**", "/swagger-ui/**", "/swagger-ui.html/**", "/v3/api-docs/**").permitAll()
					.requestMatchers("/api/v1/internal/**").permitAll()
					.requestMatchers("/actuator/health", "/actuator/info").permitAll()
					.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
					.requestMatchers("/actuator/**").hasRole("ADMIN")
					.anyRequest().authenticated()
			)
			.oauth2Login(oauth -> oauth
			        .authorizationEndpoint(authorization -> authorization
			        		.baseUri("/oauth2/authorization")
			                .authorizationRequestResolver(appRoleAwareOAuth2AuthorizationRequestResolver))
			        .redirectionEndpoint(redirection -> redirection
			        		.baseUri("/login/oauth2/code/*"))
			        .successHandler(oAuth2AuthenticationSuccessHandler)
			        .failureHandler(oAuth2AuthenticationFailureHandler)
			)
			.sessionManagement(session-> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.exceptionHandling(ex -> ex.authenticationEntryPoint(point));
			
		 
			http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
			http.headers(headers-> headers.frameOptions(frame-> frame.disable()));
		
		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowCredentials(true);
		configuration.setAllowedOriginPatterns(List.of(
				frontendUrl,
				localFrontendUrl,
				"https://*.amplifyapp.com"
		));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setExposedHeaders(List.of("Authorization", "Location", "Set-Cookie", "Content-Type"));
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
