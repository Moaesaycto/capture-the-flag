package moae.dev.Server;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import moae.dev.Utils.JwtValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class SecurityConfig {

  private final AppSecurityProperties props;

  public SecurityConfig(AppSecurityProperties props) {
    this.props = props;
  }

  @Bean
  SecurityFilterChain api(
      HttpSecurity http,
      JwtDecoder jwtDecoder,
      CorsConfigurationSource corsConfigurationSource,
      ConditionalBearerTokenResolver conditionalBearerTokenResolver)
      throws Exception {
    String[] publicPaths = props.getPublicPaths().toArray(new String[0]);

    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(publicPaths).permitAll().anyRequest().authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .bearerTokenResolver(conditionalBearerTokenResolver)
                    .jwt(
                        jwt -> {
                          jwt.jwtAuthenticationConverter(new JwtAuthenticationConverter());
                          jwt.decoder(jwtDecoder);
                        }));

    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(AppSecurityProperties securityProperties) {
    CorsConfiguration config = new CorsConfiguration();
    
    // Only allow your specific frontend - SECURE for production
    config.setAllowedOrigins(List.of(securityProperties.getFrontend()));
    
    // Allow common HTTP methods
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    
    // Allow all headers - this is safe because origin is restricted
    // Browsers send various headers (like sec-fetch-*) that need to be allowed
    config.setAllowedHeaders(List.of("*"));
    
    // Expose these headers to the frontend JavaScript
    config.setExposedHeaders(List.of("Authorization", "Content-Type"));
    
    // Allow credentials (cookies, authorization headers)
    config.setAllowCredentials(true);
    
    // Cache preflight requests for 1 hour to reduce overhead
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String secret) {
    var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
  }

  @Bean
  JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret, JwtValidator jwtValidator) {

    var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    NimbusJwtDecoder jwtDecoder =
        NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
            .build();

    OAuth2TokenValidator<Jwt> withDefaults = JwtValidators.createDefault();
    OAuth2TokenValidator<Jwt> combinedValidator =
        new DelegatingOAuth2TokenValidator<>(withDefaults, jwtValidator);

    jwtDecoder.setJwtValidator(combinedValidator);
    return jwtDecoder;
  }
}
