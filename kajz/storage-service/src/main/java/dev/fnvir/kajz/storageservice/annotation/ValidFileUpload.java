package dev.fnvir.kajz.storageservice.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dev.fnvir.kajz.storageservice.annotation.impl.FileUploadValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = FileUploadValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFileUpload {


String message() default "Invalid file type or file size";


Class<?>[] groups() default {};


Class<? extends Payload>[] payload() default {};
}
