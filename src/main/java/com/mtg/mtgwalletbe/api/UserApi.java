package com.mtg.mtgwalletbe.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtg.mtgwalletbe.api.request.AddRoleToUserRequest;
import com.mtg.mtgwalletbe.api.request.RoleRequest;
import com.mtg.mtgwalletbe.api.request.WalletUserRequest;
import com.mtg.mtgwalletbe.api.response.RoleResponse;
import com.mtg.mtgwalletbe.api.response.WalletUserCreateResponse;
import com.mtg.mtgwalletbe.entity.Role;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.service.UserService;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mtg.mtgwalletbe.security.SecurityParams.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

// TODO authorization checks on methods. Ex: who can addRoleToUser
@RestController
@RequiredArgsConstructor
public class UserApi {
    private final UserService userService;
    private final UserServiceMapper userServiceMapper;

    @PostMapping("/user/create")
    public ResponseEntity<WalletUserCreateResponse> createUser(@RequestBody @Validated WalletUserRequest walletUserRequest) throws MtgWalletGenericException {
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/create").toUriString());
        return ResponseEntity.created(uri).body(userServiceMapper.toWalletUserCreateResponse(userService.createUser(userServiceMapper.toWalletUserDto(walletUserRequest))));
    }

    @PostMapping("/role/create")
    public ResponseEntity<RoleResponse> createRole(@RequestBody @Validated RoleRequest roleRequest) throws MtgWalletGenericException {
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/role/create").toUriString());
        return ResponseEntity.created(uri).body(userServiceMapper.toRoleResponse(userService.createRole(userServiceMapper.toRoleDto(roleRequest))));
    }

    @PostMapping("/user/add-role-to-user")
    public ResponseEntity<Void> addRoleToUser(@RequestBody @Validated AddRoleToUserRequest addRoleToUserRequest) throws MtgWalletGenericException {
        userService.addRoleToUser(addRoleToUserRequest.getUsername(), addRoleToUserRequest.getRoleName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/token/refresh")
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException, MtgWalletGenericException {
        String authorizationHeader = request.getHeader(AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            try {
                String refreshToken = authorizationHeader.substring(BEARER_PREFIX.length());
                JWTVerifier verifier = JWT.require(JWT_SIGNING_ALGORITHM).build();
                DecodedJWT decodedJWT = verifier.verify(refreshToken);
                String username = decodedJWT.getSubject();
                WalletUserDto walletUserDto = userService.getUser(username);
                String accessToken = JWT.create()
                        .withSubject(walletUserDto.getUsername())
                        .withExpiresAt(new Date(System.currentTimeMillis() + JWT_ACCESS_TOKEN_EXPIRATION_DURATION * 60 * 1000))
                        .withIssuer(request.getRequestURL().toString())
                        .withClaim(JWT_TOKEN_CLAIM_KEY, walletUserDto.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                        .sign(JWT_SIGNING_ALGORITHM);
                Map<String, String> tokens = new HashMap<>();
                tokens.put(JWT_ACCESS_TOKEN_KEY, accessToken);
                tokens.put(JWT_REFRESH_TOKEN_KEY, refreshToken);
                response.setContentType(APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), tokens);
            } catch (Exception e) {
                response.setHeader("error", e.getMessage());
                response.setStatus(FORBIDDEN.value());
                Map<String, String> error = new HashMap<>();
                error.put("error_message", e.getMessage());
                response.setContentType(APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), error);
            }
        } else {
            throw new MtgWalletGenericException(GenericExceptionMessages.MISSING_REFRESH_TOKEN.getMessage());
        }
    }
}
