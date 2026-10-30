package ecommerce.security;

import ecommerce.common.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** يحدّ عدد محاولات الدخول الفاشلة لكل (مستخدم + IP) ضمن نافذة زمنية. */
@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final Duration window;
    private final ConcurrentHashMap<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public LoginAttemptService(@Value("${app.security.login.max-attempts}") int maxAttempts,
                               @Value("${app.security.login.window-seconds}") long windowSeconds) {
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    private static String key(String username, String ip) { return username.toLowerCase() + "|" + ip; }

    public void checkAllowed(String username, String ip) {
        Deque<Instant> q = failures.get(key(username, ip));
        if (q == null) return;
        synchronized (q) {
            prune(q);
            if (q.size() >= maxAttempts) {
                throw new TooManyRequestsException("Too many failed login attempts. Try again later.");
            }
        }
    }

    public void recordFailure(String username, String ip) {
        Deque<Instant> q = failures.computeIfAbsent(key(username, ip), k -> new ArrayDeque<>());
        synchronized (q) {
            prune(q);
            q.addLast(Instant.now());
        }
    }

    public void recordSuccess(String username, String ip) { failures.remove(key(username, ip)); }

    private void prune(Deque<Instant> q) {
        Instant cutoff = Instant.now().minus(window);
        while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) q.removeFirst();
    }
}
