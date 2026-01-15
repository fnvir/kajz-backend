package dev.fnvir.kajz.notificationservice.util;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.uuid.UuidValueGenerator;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;

public class UuidV7Generator implements UuidValueGenerator {

    public static final NoArgGenerator GENERATOR = Generators.timeBasedEpochRandomGenerator();

    @Override
    public UUID generateUuid(SharedSessionContractImplementor session) {
        return GENERATOR.generate();
    }
    
    public static UUID generate() {
        return GENERATOR.generate();
    }
}
