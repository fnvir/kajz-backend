package dev.fnvir.kajz.storageservice.annotation.impl;

import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;

import dev.fnvir.kajz.storageservice.annotation.ValidFileUpload;
import dev.fnvir.kajz.storageservice.dto.req.InitiateUploadRequest;
import dev.fnvir.kajz.storageservice.util.StorageFileValidatorUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FileUploadValidator implements ConstraintValidator<ValidFileUpload, InitiateUploadRequest> {

    private final StorageFileValidatorUtils fileValidator;

    @Override
    public boolean isValid(InitiateUploadRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        // Validate mime type
        if (!fileValidator.isValidMimeType(value.mimeType())) {
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
        if (!fileValidator.isValidFileSize(value.fileSize())) {
            context.buildConstraintViolationWithTemplate(
                "File size exceeds maximum allowed size"
            ).addPropertyNode("fileSize")
            .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}