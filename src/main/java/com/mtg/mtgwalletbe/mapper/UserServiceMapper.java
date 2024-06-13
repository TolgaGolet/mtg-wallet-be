package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.request.RoleRequest;
import com.mtg.mtgwalletbe.api.response.RoleResponse;
import com.mtg.mtgwalletbe.api.response.WalletUserCreateResponse;
import com.mtg.mtgwalletbe.entity.Role;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.service.dto.RoleDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserServiceMapper {
    WalletUser toWalletUserEntity(WalletUserDto walletUserDto);

    WalletUserDto toWalletUserDto(WalletUser walletUser);

    Role toRoleEntity(RoleDto roleDto);

    RoleDto toRoleDto(Role role);

    @Mapping(source = "id", target = "userId")
    WalletUserCreateResponse toWalletUserCreateResponse(WalletUserDto walletUserDto);

    RoleResponse toRoleResponse(RoleDto roleDto);

    RoleDto toRoleDto(RoleRequest roleRequest);

    void updateWalletUserFromDto(WalletUserDto dto, @MappingTarget WalletUser entity);
}
