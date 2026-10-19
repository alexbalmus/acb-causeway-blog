package com.alexbalmus.acbblog.webapp;

import java.util.List;

import org.apache.causeway.core.config.presets.CausewayPresets;
import org.apache.causeway.security.simple.realm.SimpleRealm;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Adds browser fixtures to the packaged application without shipping test users. */
public class BrowserTestApplication {
    public static void main(String[] args) {
        CausewayPresets.prototyping();
        var application = new SpringApplication(ACBBlogApp.class, Users.class);
        application.setAdditionalProfiles("browser-tests");
        application.run(args);
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("browser-tests")
    public static class Users {
        @Bean
        InitializingBean browserUsers(SimpleRealm realm, PasswordEncoder passwords) {
            return () -> {
                for (String user : List.of("wicket-writer", "wicket-other", "wicket-handles")) {
                    realm.addUser(user, passwords.encode("pass"), List.of("admin_role"));
                }
            };
        }
    }
}
