package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.request.RoleCreateRequest;
import com.mtg.mtgwalletbe.api.response.RoleCreateResponse;
import com.mtg.mtgwalletbe.entity.Role;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.service.dto.RoleDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserServiceMapper {
    WalletUser toWalletUserEntity(WalletUserBasicDto walletUserDto);

    WalletUserBasicDto toWalletUserBasicDto(WalletUser walletUser);

    Role toRoleEntity(RoleDto roleDto);

    RoleDto toRoleDto(Role role);

    RoleCreateResponse toRoleResponse(RoleDto roleDto);

    RoleDto toRoleDto(RoleCreateRequest roleCreateRequest);
}
