package com.mtg.mtgwalletbe.api;

import com.mtg.mtgwalletbe.api.request.AddRoleToUserRequest;
import com.mtg.mtgwalletbe.api.request.RoleCreateRequest;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.repository.RoleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// @PreAuthorize("hasAuthority('ADMIN')") only works through the Spring proxy, so this runs the real context (H2) and
// calls the UserApi bean with a hand-set security context; each test rolls back
@SpringBootTest
@ActiveProfiles("unittest")
@Transactional
class UserApiAuthorizationTest {

    @Autowired private UserApi userApi;
    @Autowired private RoleRepository roleRepository;
    @Autowired private EntityManager entityManager;

    private final RoleCreateRequest auditor = roleRequest("AUDITOR");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void loginWith(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("someone", null,
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    private static RoleCreateRequest roleRequest(String name) {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setName(name);
        return request;
    }

    @Test
    void createRole_asRegularUser_isDenied_andWritesNothing() {
        loginWith("USER");

        assertThatThrownBy(() -> userApi.createRole(auditor)).isInstanceOf(AccessDeniedException.class);
        assertThat(roleRepository.findByName("AUDITOR")).isEmpty();
    }

    @Test
    void createRole_withoutAuthorities_isDenied() {
        loginWith();

        assertThatThrownBy(() -> userApi.createRole(auditor)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createRole_asAdmin_createsTheRole() throws Exception {
        loginWith("ADMIN");

        var response = userApi.createRole(roleRequest("AUDITOR"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(roleRepository.findByName("AUDITOR")).isPresent();
    }

    @Test
    void addRoleToUser_asRegularUser_isDenied_andTheUserKeepsItsRoles() {
        WalletUser target = WalletUser.builder().username("bob").email("bob@example.com").name("bob").password("hash")
                .isDefaultsCreated(true).isEmailVerified(true).totpEnabled(false).build();
        entityManager.persist(target);
        loginWith("USER");
        AddRoleToUserRequest request = new AddRoleToUserRequest();
        request.setUsername("bob");
        request.setRoleName("ADMIN");

        assertThatThrownBy(() -> userApi.addRoleToUser(request)).isInstanceOf(AccessDeniedException.class);
        assertThat(target.getRoles()).isEmpty();
    }

    // The authority check is exact: "admin" or "ROLE_ADMIN" don't count
    @Test
    void createRole_withSimilarButDifferentAuthority_isDenied() {
        loginWith("admin", "ROLE_ADMIN");

        assertThatThrownBy(() -> userApi.createRole(auditor)).isInstanceOf(AccessDeniedException.class);
        assertThat(roleRepository.findByName("AUDITOR")).isEmpty();
    }
}
