package dev.fnvir.kajz.authservice.service;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.stereotype.Service;

import dev.fnvir.kajz.authservice.dto.UserDTO;
import dev.fnvir.kajz.authservice.dto.req.UserSignupRequest;
import dev.fnvir.kajz.authservice.exception.ConflictException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final KeycloakService keycloakService;

    public UserDTO register(@Valid UserSignupRequest signupReq) {
        if (Period.between(signupReq.dateOfBirth(), LocalDate.now()).getYears() < 18)
            throw new ConflictException("User must be at least 18 years old.");
        
        var user = keycloakService.createUser(signupReq);
        
        //TODO: send verification email
        
        return user;
    }

}
