package dev.fnvir.kajz.storageservice;

import org.springframework.boot.SpringApplication;

public class TestStorageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(StorageServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
