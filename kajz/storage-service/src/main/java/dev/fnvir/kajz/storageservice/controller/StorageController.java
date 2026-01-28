package dev.fnvir.kajz.storageservice.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.fnvir.kajz.storageservice.dto.req.CompleteUploadRequest;
import dev.fnvir.kajz.storageservice.dto.req.InitiateUploadRequest;
import dev.fnvir.kajz.storageservice.dto.res.CompleteUploadResponse;
import dev.fnvir.kajz.storageservice.dto.res.InitiateUploadResponse;
import dev.fnvir.kajz.storageservice.service.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping(path = "/storage", version = "1")
@RequiredArgsConstructor
public class StorageController {
    
    private final StorageService storageService;
    
    @PostMapping("/initiate-upload")
    public Mono<ResponseEntity<InitiateUploadResponse>> initiateUpload(
            @RequestBody @Valid InitiateUploadRequest req,
            Authentication authentication
    ) {
        return Mono.fromCallable(() -> {
            UUID userId = UUID.fromString(authentication.getName());
            return ResponseEntity.ok(storageService.initiateUploadProcess(userId, req));
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    @PostMapping("/complete-upload")
    public Mono<CompleteUploadResponse> completeUpload(
            @RequestBody @Valid CompleteUploadRequest req,
            Authentication authentication
    ) {
        return Mono.fromCallable(() -> {
            UUID userId = UUID.fromString(authentication.getName());
            return storageService.verifyAndCompleteUpload(userId, req);
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    

}
