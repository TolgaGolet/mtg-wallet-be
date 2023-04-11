package com.mtg.mtgwalletbe.security;

import com.mtg.mtgwalletbe.security.dto.CustomAuthenticationFilterConstructorDTO;
import com.mtg.mtgwalletbe.security.filter.CustomAuthenticationFilter;
import com.mtg.mtgwalletbe.security.filter.CustomAuthorizationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static com.mtg.mtgwalletbe.security.SecurityParams.AUTH_WHITELIST;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Profile("!disabled-security")
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private final UserDetailsService userDetailsService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    @Value("${mtgWallet.security.jwtSecretKey}")
    private String jwtSecretKey;
    @Value("${mtgWallet.security.jwtAccessTokenExpirationDuration}")
    private int jwtAccessTokenExpirationDuration;
    @Value("${mtgWallet.security.jwtRefreshTokenExpirationDuration}")
    private int jwtRefreshTokenExpirationDuration;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults());
        http.csrf().disable();
        http.sessionManagement().sessionCreationPolicy(STATELESS);
        http.authorizeRequests().antMatchers(AUTH_WHITELIST).permitAll();
        //---------------------------------------
        // configurations here
        // example: http.authorizeRequests().antMatchers(HttpMethod.GET, "path/**").hasAnyAuthority("ROLE_USER");
        //---------------------------------------
        http.authorizeRequests().anyRequest().authenticated();
        http.addFilter(new CustomAuthenticationFilter(CustomAuthenticationFilterConstructorDTO.builder().authenticationManager(authenticationManagerBean()).jwtSecretKey(jwtSecretKey).jwtAccessTokenExpirationDuration(jwtAccessTokenExpirationDuration).jwtRefreshTokenExpirationDuration(jwtRefreshTokenExpirationDuration).build()));
        http.addFilterBefore(new CustomAuthorizationFilter(jwtSecretKey), UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "https://test-mtg-wallet-fe.up.railway.app"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
