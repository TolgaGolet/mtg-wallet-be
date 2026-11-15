package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.request.RoleCreateRequest;
import com.mtg.mtgwalletbe.api.response.RoleCreateResponse;
import com.mtg.mtgwalletbe.entity.Role;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.service.dto.RoleDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MtgMapperConfig.class)
public interface UserServiceMapper {
    WalletUserBasicDto toWalletUserBasicDto(WalletUser walletUser);

    Role toRoleEntity(RoleDto roleDto);

    RoleDto toRoleDto(Role role);

    RoleCreateResponse toRoleResponse(RoleDto roleDto);

    @Mapping(target = "id", ignore = true) // assigned when the role is saved
    RoleDto toRoleDto(RoleCreateRequest roleCreateRequest);
}
