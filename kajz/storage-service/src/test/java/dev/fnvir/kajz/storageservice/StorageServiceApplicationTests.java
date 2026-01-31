package dev.fnvir.kajz.storageservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import dev.fnvir.kajz.storageservice.service.AbstractStorageProvider;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class StorageServiceApplicationTests {
    
    @MockitoBean
    AbstractStorageProvider abstractStorageProvider;

    @Test
    void contextLoads() {
    }

}
