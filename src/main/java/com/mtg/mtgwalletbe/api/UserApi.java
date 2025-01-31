package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.AddRoleToUserRequest;
import com.mtg.mtgwalletbe.api.request.ChangePasswordRequest;
import com.mtg.mtgwalletbe.api.request.RoleCreateRequest;
import com.mtg.mtgwalletbe.api.response.RoleCreateResponse;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserApi {
    private final UserService userService;
    private final UserServiceMapper userServiceMapper;

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/role/create")
    public ResponseEntity<RoleCreateResponse> createRole(@RequestBody @Validated RoleCreateRequest roleCreateRequest) throws MtgWalletGenericException {
        return ResponseEntity.ok(userServiceMapper.toRoleResponse(userService.createRole(userServiceMapper.toRoleDto(roleCreateRequest))));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/user/add-role-to-user")
    public ResponseEntity<Void> addRoleToUser(@RequestBody @Validated AddRoleToUserRequest addRoleToUserRequest) throws MtgWalletGenericException {
        userService.addRoleToUser(addRoleToUserRequest.getUsername(), addRoleToUserRequest.getRoleName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/user/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody @Validated ChangePasswordRequest request) throws MtgWalletGenericException {
        userService.changePassword(request);
        return ResponseEntity.ok().build();
    }
}
