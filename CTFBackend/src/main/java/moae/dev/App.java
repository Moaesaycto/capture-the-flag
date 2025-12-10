package moae.dev;

import moae.dev.Server.AppConfig;
import moae.dev.Services.PushNotificationService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;
import moae.dev.Game.Game;

import java.security.Security;

@SpringBootApplication
@RestController
public class App {

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        SpringApplication.run(App.class, args);
    }

    @Bean
    public Game game(AppConfig config, PushNotificationService pushNotificationService) {
        return new Game(config, pushNotificationService);
    }
}