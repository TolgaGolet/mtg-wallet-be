package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.ChangePasswordRequest;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.repository.RoleRepository;
import com.mtg.mtgwalletbe.repository.WalletUserRepository;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    private UserServiceMapper mockMapper;
    private WalletUserRepository mockWalletUserRepository;
    private PasswordEncoder mockPasswordEncoder;
    private Authentication mockAuthentication;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        mockMapper = mock(UserServiceMapper.class);
        mockWalletUserRepository = mock(WalletUserRepository.class);
        RoleRepository mockRoleRepository = mock(RoleRepository.class);
        mockPasswordEncoder = mock(PasswordEncoder.class);
        mockAuthentication = mock(Authentication.class);
        SecurityContext mockSecurityContext = mock(SecurityContext.class);

        userService = new UserServiceImpl(mockMapper, mockWalletUserRepository,
                mockRoleRepository, mockPasswordEncoder);

        SecurityContextHolder.setContext(mockSecurityContext);
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
    }

    @Test
    void changePassword_ValidCurrentPassword_UpdatesToNewPassword() throws MtgWalletGenericException {
        // Arrange
        String username = "testUser";
        String currentPassword = "oldPass";
        String newPassword = "newPass";
        String encodedOldPassword = "encodedOldPass";
        String encodedNewPassword = "encodedNewPass";

        WalletUser existingUser = new WalletUser();
        existingUser.setUsername(username);
        existingUser.setPassword(encodedOldPassword);

        WalletUserDto userDto = WalletUserDto.builder()
                .username(username)
                .password(encodedOldPassword)
                .build();

        // Setup security context
        when(mockAuthentication.getName()).thenReturn(username);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);

        // Setup password encoding
        when(mockPasswordEncoder.matches(currentPassword, encodedOldPassword)).thenReturn(true);
        when(mockPasswordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);

        // Setup user retrieval
        when(mockWalletUserRepository.findByUsername(username))
                .thenReturn(Optional.of(existingUser));
        when(mockMapper.toWalletUserDto(existingUser)).thenReturn(userDto);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .confirmPassword(newPassword)
                .build();

        // Act
        userService.changePassword(request);

        // Assert
        verify(mockPasswordEncoder).matches(currentPassword, encodedOldPassword);
        verify(mockPasswordEncoder).encode(newPassword);

        ArgumentCaptor<WalletUserDto> dtoCaptor = ArgumentCaptor.forClass(WalletUserDto.class);
        verify(mockMapper).updateWalletUserFromDto(dtoCaptor.capture(), any(WalletUser.class));
        assertEquals(encodedNewPassword, dtoCaptor.getValue().getPassword());
    }

    @Test
    void getCurrentLoggedInUserFull_AuthenticatedUser_ReturnsUserDto() {
        // Arrange
        String username = "testUser";
        WalletUser user = new WalletUser();
        WalletUserDto userDto = WalletUserDto.builder().build();

        when(mockAuthentication.getName()).thenReturn(username);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockWalletUserRepository.findByUsername(username))
                .thenReturn(Optional.of(user));
        when(mockMapper.toWalletUserDto(user)).thenReturn(userDto);

        // Act
        WalletUserDto result = userService.getCurrentLoggedInUserFull();

        // Assert
        assertNotNull(result);
        verify(mockWalletUserRepository).findByUsername(username);
        verify(mockMapper).toWalletUserDto(user);
    }
}