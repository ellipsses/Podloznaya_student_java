package com.example.student.security;

import com.example.student.model.Role;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Фильтр проверяется без заглушек: настоящий JwtService и простые самодельные реализации интерфейсов. */
class JwtFilterTest {

    /** Запоминает, передал ли фильтр запрос дальше. */
    private static class RecordingChain implements FilterChain {
        boolean called;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            called = true;
        }
    }

    private final JwtService jwtService = new JwtService();
    private final RecordingChain chain = new RecordingChain();
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("GET", "/api/v1/student");
        request.setRequestURI("/api/v1/student");
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static UserDetails details(String email, String role) {
        return User.withUsername(email).password("x").roles(role).build();
    }

    /** "База пользователей" из одного человека. */
    private static UserDetailsService usersWith(String email, String role) {
        return username -> {
            if (!username.equals(email)) {
                throw new UsernameNotFoundException(username);
            }
            return details(email, role);
        };
    }

    @Test
    @DisplayName("Без заголовка Authorization запрос идёт дальше без входа")
    void noHeaderPassesThroughUnauthenticated() throws Exception {
        new JwtFilter(usersWith("admin@example.com", "ADMIN"), jwtService).doFilter(request, response, chain);

        assertThat(chain.called).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Заголовок не Bearer (например Basic) игнорируется")
    void nonBearerHeaderIsIgnored() throws Exception {
        request.addHeader("Authorization", "Basic YWRtaW46YWRtaW4=");

        new JwtFilter(usersWith("admin@example.com", "ADMIN"), jwtService).doFilter(request, response, chain);

        assertThat(chain.called).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Верный токен: пользователь входит с нужными ролями")
    void validTokenAuthenticatesUser() throws Exception {
        request.addHeader("Authorization", "Bearer "
                + jwtService.generateToken("admin@example.com", Set.of(Role.ADMIN)));

        new JwtFilter(usersWith("admin@example.com", "ADMIN"), jwtService).doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("admin@example.com");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
        assertThat(chain.called).isTrue();
    }

    @Test
    @DisplayName("Если пользователь уже вошёл, фильтр не подменяет его вход")
    void existingAuthenticationIsKept() throws Exception {
        Authentication existing = new TestingAuthenticationToken("other@example.com", null, "ROLE_STUDENT");
        SecurityContextHolder.getContext().setAuthentication(existing);
        request.addHeader("Authorization", "Bearer "
                + jwtService.generateToken("admin@example.com", Set.of(Role.ADMIN)));

        new JwtFilter(usersWith("admin@example.com", "ADMIN"), jwtService).doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        assertThat(chain.called).isTrue();
    }

    @Test
    @DisplayName("Токен другого пользователя не даёт входа")
    void tokenOfAnotherUserDoesNotAuthenticate() throws Exception {
        request.addHeader("Authorization", "Bearer "
                + jwtService.generateToken("admin@example.com", Set.of(Role.ADMIN)));
        // по этому email "база" возвращает другого человека
        UserDetailsService mismatch = username -> details("someone-else@example.com", "ADMIN");

        new JwtFilter(mismatch, jwtService).doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.called).isTrue();
    }

    @Test
    @DisplayName("Плохой токен на /api/: ответ 401 с JSON, дальше запрос не идёт")
    void badTokenOnApiGives401() throws Exception {
        request.addHeader("Authorization", "Bearer broken.token.value");

        new JwtFilter(usersWith("admin@example.com", "ADMIN"), jwtService).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("error");
        assertThat(chain.called).isFalse();
    }

    @Test
    @DisplayName("Токен есть, а пользователя уже нет в базе: 401")
    void unknownUserGives401() throws Exception {
        request.addHeader("Authorization", "Bearer "
                + jwtService.generateToken("ghost@example.com", Set.of(Role.STUDENT)));

        new JwtFilter(usersWith("admin@example.com", "ADMIN"), jwtService).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.called).isFalse();
    }

    @Test
    @DisplayName("Плохой токен не на /api/: ошибка пробрасывается дальше")
    void badTokenOutsideApiRethrows() {
        request.setRequestURI("/index.html");
        request.addHeader("Authorization", "Bearer broken.token.value");
        JwtFilter filter = new JwtFilter(usersWith("admin@example.com", "ADMIN"), jwtService);

        assertThatThrownBy(() -> filter.doFilter(request, response, chain)).isInstanceOf(JwtException.class);
    }
}
