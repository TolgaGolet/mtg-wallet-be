package com.mtg.mtgwalletbe.security.service;

import com.mtg.mtgwalletbe.entity.UserToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.TokenType;
import com.mtg.mtgwalletbe.repository.UserTokenRepository;
import com.mtg.mtgwalletbe.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// Regression: logout revoked only the access token, leaving the refresh token usable
class LogoutServiceTest {

    private final UserTokenRepository repository = mock(UserTokenRepository.class);
    private final UserService userService = mock(UserService.class);
    private final LogoutService logoutService = new LogoutService(repository, userService);

    @Test
    void logout_revokesAccessAndRefreshTokensOfTheUser() {
        WalletUser user = WalletUser.builder().id(1L).build();
        UserToken access = UserToken.builder().token("access").tokenType(TokenType.BEARER).user(user).build();
        when(repository.findByToken("access")).thenReturn(Optional.of(access));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access");

        logoutService.logout(request, new MockHttpServletResponse(), null);

        // revokeAllSessions covers the access token and the refresh token issued with it
        verify(userService).revokeAllSessions(1L);
    }
}
