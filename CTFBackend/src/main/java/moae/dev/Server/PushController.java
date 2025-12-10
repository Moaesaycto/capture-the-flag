package moae.dev.Server;

import moae.dev.Services.PushNotificationService;
import moae.dev.Utils.PushSubscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/push")
public class PushController {

  @Autowired private PushNotificationService pushService;

  @Value("${vapid.publicKey}")
  private String publicKey;

  @GetMapping("/key")
  public Map<String, String> getPublicKey() {
    return Map.of("publicKey", publicKey);
  }

  @PostMapping("/subscribe")
  public void subscribe(@RequestBody PushSubscription subscription) {
    pushService.addSubscription(subscription.endpoint, subscription);
  }

  @PostMapping("/unsubscribe")
  public void unsubscribe(@RequestBody Map<String, String> body) {
    pushService.removeSubscription(body.get("endpoint"));
  }
}
