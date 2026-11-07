package team.projectpulse.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;

@Component
public class ActiveAccountJwtValidator implements OAuth2TokenValidator<Jwt> {

    private final UserRepository userRepository;

    public ActiveAccountJwtValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Object userId = token.getClaim("userId");
        if (userId instanceof Number id && id.longValue() > 0 && id.longValue() <= Integer.MAX_VALUE
                && this.userRepository.findById(id.intValue()).filter(PeerEvaluationUser::isEnabled).isPresent()) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Account is unavailable.", null));
    }
}
