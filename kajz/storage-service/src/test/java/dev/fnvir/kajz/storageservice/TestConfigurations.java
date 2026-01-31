package dev.fnvir.kajz.storageservice;

import java.util.Optional;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.NoOpTaskScheduler;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;

@TestConfiguration
public class TestConfigurations {
    
    @Bean
    LockProvider NoopShedLockProvider() {
        return new LockProvider() {
            @Override
            public Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
                return Optional.of(() -> {});
            }
        };
    }
    
    @Bean
    TaskScheduler disableTaskScheduler() {
        return new NoOpTaskScheduler();
    }

}
