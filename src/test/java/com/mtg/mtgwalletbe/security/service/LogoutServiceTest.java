package com.mtg.mtgwalletbe.security.service;

import com.mtg.mtgwalletbe.entity.UserToken;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.TokenType;
import com.mtg.mtgwalletbe.repository.UserTokenRepository;
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
    private final LogoutService logoutService = new LogoutService(repository);

    @Test
    void logout_revokesAccessAndRefreshTokensOfTheUser() {
        WalletUser user = WalletUser.builder().id(1L).build();
        UserToken access = UserToken.builder().token("access").tokenType(TokenType.BEARER).user(user).build();
        UserToken refresh = UserToken.builder().token("refresh").tokenType(TokenType.REFRESH).user(user).build();
        when(repository.findByToken("access")).thenReturn(Optional.of(access));
        when(repository.findAllNotFullyRevokedTokensByUserId(1L)).thenReturn(List.of(access, refresh));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access");

        logoutService.logout(request, new MockHttpServletResponse(), null);

        assertThat(List.of(access, refresh)).noneMatch(UserToken::isActive);
        verify(repository).saveAll(List.of(access, refresh));
    }
}
