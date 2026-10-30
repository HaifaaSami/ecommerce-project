package ecommerce.config;

import ecommerce.user.Role;
import ecommerce.user.User;
import ecommerce.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** ينشئ حساب المسؤول عند أول تشغيل (التسجيل العام لا يمنح صلاحية ADMIN أبداً). */
@Configuration
public class AdminInitializer {
    @Bean
    CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder encoder,
                                @Value("${app.admin.username}") String username,
                                @Value("${app.admin.password}") String password) {
        return args -> {
            if (!users.existsByUsername(username)) {
                users.save(new User(username, encoder.encode(password), Role.ADMIN));
            }
        };
    }
}
