package dev.fnvir.kajz.storageservice.controller;

import java.time.Duration;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ETag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import dev.fnvir.kajz.storageservice.dto.StreamFileDto;
import dev.fnvir.kajz.storageservice.dto.req.CompleteUploadRequest;
import dev.fnvir.kajz.storageservice.dto.req.InitiateUploadRequest;
import dev.fnvir.kajz.storageservice.dto.res.CompleteUploadResponse;
import dev.fnvir.kajz.storageservice.dto.res.InitiateUploadResponse;
import dev.fnvir.kajz.storageservice.service.StorageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/storage", version = "1")
@RequiredArgsConstructor
public class StorageController {
    
    private final StorageService storageService;
    
    @PostMapping("/initiate-upload")
    public ResponseEntity<InitiateUploadResponse> initiateUpload(
            @RequestBody @Valid InitiateUploadRequest req,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(storageService.initiateUploadProcess(userId, req));
    }
    
    @PostMapping("/complete-upload")
    public CompleteUploadResponse completeUpload(
            @RequestBody @Valid CompleteUploadRequest req,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return storageService.verifyAndCompleteUpload(userId, req);
    }
    
    /**
     * Serve a file by its ID.
     * 
     * <ul>
     * <li>If the file access is public, no authentication is required.</li>
     * <li>If the file access is protected, the user must be authenticated.</li>
     * <li>If the file access is private, the user must be authenticated and must be
     * the owner of the file.</li>
     * </ul>
     * 
     * @param fileId      the ID of the file to serve.
     * @param ifNoneMatch the ETag from the client for cache validation.
     * @param response    the server HTTP response to write to.
     * @return 200 OK with file stream, 401 Unauthorized, 403 Forbidden, 304 Not
     *         Modified, or 404 Not Found.
     */
    @SecurityRequirements
    @GetMapping(path = "/download/{fileId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> serveFileValidatingAccess(
            @PathVariable Long fileId,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch,
            HttpServletResponse response
    ) {
        StreamFileDto result = storageService.downloadFileValidatingAccess(fileId, ifNoneMatch);
        
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (ifNoneMatch != null && result.getEtag() != null
                && ETag.create(ifNoneMatch).equals(ETag.create(result.getEtag()))
        ) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(result.getFilename()).build().toString())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic().mustRevalidate())
                .contentLength(result.getContentLength())
                .contentType(result.getMediaType())
                .eTag(result.getEtag())
                .body(result.streamFile());
    }
    
}
