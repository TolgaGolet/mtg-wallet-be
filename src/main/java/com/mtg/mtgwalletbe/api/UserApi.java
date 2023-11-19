package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.AddRoleToUserRequest;
import com.mtg.mtgwalletbe.api.request.ChangePasswordRequest;
import com.mtg.mtgwalletbe.api.request.RoleRequest;
import com.mtg.mtgwalletbe.api.response.RoleResponse;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;

// TODO authorization checks on methods. Ex: who can addRoleToUser
@RestController
@RequiredArgsConstructor
public class UserApi {
    private final UserService userService;
    private final UserServiceMapper userServiceMapper;

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

    @PatchMapping
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request, Principal connectedUser) {
        userService.changePassword(request, connectedUser);
        return ResponseEntity.ok().build();
    }
}
