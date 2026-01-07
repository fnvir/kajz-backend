package dev.fnvir.kajz.authservice.dto.req;

import java.time.LocalDate;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record UserSignupRequest(
        @NotBlank
        @Size(min = 1, max=50, message = "First name should be between 1 and 50 characters")
        @JsonAlias("firstname")
        String firstName,
        
        @NotBlank
        @Size(min = 1, max=50, message = "Last name should be between 1 and 50 characters")
        @JsonAlias("lastname")
        String lastName,
        
        @NotBlank
        @Email
        String email,
        
        @NotBlank
        @Size(min = 8, message = "Password should contain atleast 8 characters")
        String password,
        
        @NotNull
        @PastOrPresent
        LocalDate dateOfBirth
) {
    
    public String username() {
        return String.join("_", 
                StringUtils.substring(firstName, 0, 10),
                StringUtils.substring(lastName, 0, 10),
                UUID.randomUUID().toString().substring(0, 8)
        );
    }
    
}