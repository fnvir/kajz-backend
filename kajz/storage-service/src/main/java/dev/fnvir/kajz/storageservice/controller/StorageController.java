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
import dev.fnvir.kajz.storageservice.dto.res.ErrorResponse;
import dev.fnvir.kajz.storageservice.dto.res.InitiateUploadResponse;
import dev.fnvir.kajz.storageservice.dto.res.PreSignedDownloadUrlResponse;
import dev.fnvir.kajz.storageservice.service.StorageService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/storage", version = "1")
@RequiredArgsConstructor
public class StorageController {
    
    private final StorageService storageService;
    
    /**
     * Initiate an upload process by generating a pre-signed URL for uploading.
     * 
     * @param req            the initiate upload request payload
     * @param authentication the authentication object
     * @return response containing the pre-signed URL and upload details
     */
    @PostMapping("/initiate-upload")
    public ResponseEntity<InitiateUploadResponse> initiateUpload(
            @RequestBody @Valid InitiateUploadRequest req,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(storageService.initiateUploadProcess(userId, req));
    }
    
    /**
     * Complete an upload by verifying and validating the uploaded file.
     * 
     * @param req            the complete upload request
     * @param authentication the authentication object
     * @return response containing the upload completion result
     */
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
     * <p>
     * NOTE: This endpoint is only to be used if a CDN has not been set up yet, and
     * public access to blobs are disabled.
     * </p>
     * 
     * <ul>
     *   <li>If the file access is public, no authentication is required.</li>
     *   <li>If the file access is protected, the user must be authenticated.</li>
     *   <li>If the file access is private, the user must be authenticated and must be
     *       the owner of the file.</li>
     *   <li>User's with ADMIN role can access all files.</li>
     * </ul>
     * 
     * @param fileId      the ID of the file to serve.
     * @param ifNoneMatch the ETag from the client for cache validation.
     * @param response    the server HTTP response to write to.
     * @return the file stream.
     */
    @GetMapping(path = "/download/{fileId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "A stream of the file", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized (for protected/private files)", content = @Content)
    @ApiResponse(responseCode = "403", description = "Forbidden (for private files)", content = @Content)
    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<StreamingResponseBody> serveFileValidatingAccess(
            @PathVariable Long fileId,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
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

    /**
     * Generate a pre-signed temporary URL for downloading a file.
     * 
     * Only file owners can generate pre-signed URLs for their files. 
     * 
     * <strong> This endpoint is recommended for downloading/accessing private files. </strong>
     * 
     * @param fileId         the ID of the file
     * @param authentication the authentication object
     * @return response containing the pre-signed download URL
     */
    @GetMapping("/files/{fileId}/temp-url")
    public ResponseEntity<PreSignedDownloadUrlResponse> generateTempDownloadUrl(
            @PathVariable Long fileId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(storageService.generateTempDownloadUrl(fileId, userId));
    }
}
