package team.projectpulse.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import team.projectpulse.student.Student;
import team.projectpulse.user.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActiveAccountJwtValidatorTest {

    @Test
    void sameTokenFailsAsSoonAsTheAccountIsDisabled() {
        UserRepository users = mock(UserRepository.class);
        Student student = new Student();
        student.setEnabled(true);
        when(users.findById(4)).thenReturn(Optional.of(student));
        Jwt token = Jwt.withTokenValue("test").header("alg", "RS256").claim("userId", 4L).build();
        ActiveAccountJwtValidator validator = new ActiveAccountJwtValidator(users);

        assertThat(validator.validate(token).hasErrors()).isFalse();
        student.setEnabled(false);
        assertThat(validator.validate(token).hasErrors()).isTrue();
    }
}
