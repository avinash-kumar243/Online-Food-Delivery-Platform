package com.quickbite.microservice;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Properties;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Path DEFAULT_STORE = Paths.get(System.getProperty("user.dir"), "admin-store.properties");

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String enabled = System.getenv("ADMIN_BOOTSTRAP_ENABLED");
        if (!"true".equalsIgnoreCase(enabled)) {
            return; // bootstrap not enabled
        }

        String storePath = System.getenv("ADMIN_STORE_PATH");
        Path store = storePath != null ? Paths.get(storePath) : DEFAULT_STORE;

        if (Files.exists(store)) {
            System.out.println("Admin bootstrap: store already exists at " + store.toAbsolutePath() + ", skipping.");
            return;
        }

        String adminEmail = System.getenv("ADMIN_EMAIL");
        String adminPassword = System.getenv("ADMIN_PASSWORD");

        if (adminEmail == null || adminPassword == null) {
            System.err.println("ADMIN_BOOTSTRAP_ENABLED=true but ADMIN_EMAIL or ADMIN_PASSWORD not set; skipping bootstrap.");
            return;
        }

        Properties p = new Properties();
        p.setProperty("email", adminEmail);
        p.setProperty("passwordHash", passwordEncoder.encode(adminPassword));
        p.setProperty("createdAt", Instant.now().toString());

        Files.createDirectories(store.getParent() == null ? Paths.get(".") : store.getParent());
        try (OutputStream out = Files.newOutputStream(store, StandardOpenOption.CREATE_NEW)) {
            p.store(out, "Admin bootstrap (one-time)");
        }

        System.out.println("Admin bootstrap: created admin store at " + store.toAbsolutePath());
    }
}
