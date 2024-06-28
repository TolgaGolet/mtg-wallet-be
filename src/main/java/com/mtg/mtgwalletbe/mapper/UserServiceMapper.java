package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.request.RoleCreateRequest;
import com.mtg.mtgwalletbe.api.response.RoleCreateResponse;
import com.mtg.mtgwalletbe.api.response.WalletUserCreateResponse;
import com.mtg.mtgwalletbe.entity.Role;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.service.dto.RoleDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserServiceMapper {
    WalletUser toWalletUserEntity(WalletUserDto walletUserDto);

    WalletUser toWalletUserEntity(WalletUserBasicDto walletUserDto);

    WalletUserDto toWalletUserDto(WalletUser walletUser);

    WalletUserBasicDto toWalletUserBasicDto(WalletUser walletUser);

    Role toRoleEntity(RoleDto roleDto);

    RoleDto toRoleDto(Role role);

    @Mapping(source = "id", target = "userId")
    WalletUserCreateResponse toWalletUserCreateResponse(WalletUserDto walletUserDto);

    RoleCreateResponse toRoleResponse(RoleDto roleDto);

    RoleDto toRoleDto(RoleCreateRequest roleCreateRequest);

    void updateWalletUserFromDto(WalletUserDto dto, @MappingTarget WalletUser entity);
}
