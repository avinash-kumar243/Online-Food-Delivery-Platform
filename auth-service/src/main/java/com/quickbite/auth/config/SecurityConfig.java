package com.quickbite.auth.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
		session should be stateless 
*/
	
	
	private final JwtFilter filter;  
	private final JWTAuthenticationEntryPoint point; 
	private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
	
	@Value("${frontend-url}")
    private String frontendUrl; 
	
	@Value("${backend-url}")
    private String backendUrl; 
	
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
		 	.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(authz -> authz
					.requestMatchers("/auth/**", "/oauth2/**", "/h2-console/**", "/swagger-ui/**", "/swagger-ui.html/**", "/v3/api-docs/**").permitAll()
					.requestMatchers("/actuator/health", "/actuator/info").permitAll()
					.requestMatchers("/actuator/**").hasRole("ADMIN")
					.anyRequest().authenticated()
			)
			.oauth2Login(oauth -> oauth
			        .successHandler(oAuth2AuthenticationSuccessHandler)
//			        .failureUrl(frontendUrl + "/auth?oauth2=failed")
			)
			.sessionManagement(session-> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(ex -> ex.authenticationEntryPoint(point));
			
		 
			http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
			http.headers(headers-> headers.frameOptions(frame-> frame.disable()));
		
		return http.build();
	}
	 	 
	
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {	
		CorsConfiguration configuration = new CorsConfiguration();
		
		configuration.setAllowedOrigins(Arrays.asList(frontendUrl, backendUrl));
		
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		
		configuration.setAllowedHeaders(Arrays.asList("*"));
		configuration.setAllowCredentials(true);
		
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source; 
	}
}