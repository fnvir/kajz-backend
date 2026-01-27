package dev.fnvir.kajz.storageservice.annotation.impl;

import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;

import dev.fnvir.kajz.storageservice.annotation.ValidFileUpload;
import dev.fnvir.kajz.storageservice.config.StorageProperties;
import dev.fnvir.kajz.storageservice.dto.req.InitiateUploadRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@Component
public class FileUploadValidator implements ConstraintValidator<ValidFileUpload, InitiateUploadRequest> {

    private final StorageProperties storageProperties;

    public FileUploadValidator(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public boolean isValid(InitiateUploadRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        // Validate mime type
        if (!storageProperties.getAllowedTypes().contains(value.mimeType())) {
            context.buildConstraintViolationWithTemplate(
                "Unsupported file type: " + value.mimeType()
            )
            .addPropertyNode("mimeType")
            .addConstraintViolation();
            valid = false;
        }
        
        //Validate file name
        var filenameMimeType = MediaTypeFactory.getMediaType(value.filename());
        if (filenameMimeType.isPresent() && !filenameMimeType.get().toString().equals(value.mimeType())) {
            context.buildConstraintViolationWithTemplate(
                    "Media type of filename doesn't match the provided mimeType"
                )
                .addPropertyNode("filename")
                .addConstraintViolation();
                valid = false;
        }

        // Validate file size
        if (value.fileSize() > storageProperties.getMaxSize()) {
            context.buildConstraintViolationWithTemplate(
                "File size exceeds maximum allowed size"
            ).addPropertyNode("fileSize")
            .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}