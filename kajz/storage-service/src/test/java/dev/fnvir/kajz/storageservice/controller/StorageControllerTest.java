package dev.fnvir.kajz.storageservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import dev.fnvir.kajz.storageservice.config.SecurityConfig;
import dev.fnvir.kajz.storageservice.dto.StreamFileDto;
import dev.fnvir.kajz.storageservice.dto.req.CompleteUploadRequest;
import dev.fnvir.kajz.storageservice.dto.req.InitiateUploadRequest;
import dev.fnvir.kajz.storageservice.dto.res.CompleteUploadResponse;
import dev.fnvir.kajz.storageservice.dto.res.FileUploadResponse;
import dev.fnvir.kajz.storageservice.dto.res.InitiateUploadResponse;
import dev.fnvir.kajz.storageservice.dto.res.PreSignedDownloadUrlResponse;
import dev.fnvir.kajz.storageservice.enums.FileAccessLevel;
import dev.fnvir.kajz.storageservice.enums.UploadStatus;
import dev.fnvir.kajz.storageservice.service.StorageService;

/**
 * Unit tests for {@link StorageController}.
 * Tests controller methods directly without full Spring MVC context.
 */
@ExtendWith(MockitoExtension.class)
@Import({SecurityConfig.class})
class StorageControllerTest {

    @Mock
    private StorageService storageService;

    @InjectMocks
    private StorageController storageController;

    private UUID testUserId;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        authentication = new TestingAuthenticationToken(testUserId.toString(), null, "ROLE_USER");
    }

    @Nested
    @DisplayName("initiateUpload tests")
    class InitiateUploadTests {

        @Test
        @DisplayName("should return 200 with presigned URL when valid request")
        void shouldReturnPresignedUrlForValidRequest() {
            InitiateUploadRequest request = new InitiateUploadRequest(
                    "document.pdf",
                    "application/pdf",
                    2048L,
                    FileAccessLevel.PRIVATE,
                    "user-doc"
            );
            
            InitiateUploadResponse expectedResponse = InitiateUploadResponse.builder()
                    .fileId(12345L)
                    .uploadUrl("https://storage.example.com/upload")
                    .expiresAt(Instant.now().plusSeconds(300))
                    .uploadHeaders(Map.of("x-amz-acl", "private"))
                    .build();
            
            when(storageService.initiateUploadProcess(eq(testUserId), any(InitiateUploadRequest.class)))
                    .thenReturn(expectedResponse);
            
            ResponseEntity<InitiateUploadResponse> result = storageController.initiateUpload(request, authentication);
            
            assertNotNull(result);
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(12345L, result.getBody().fileId());
            assertEquals("https://storage.example.com/upload", result.getBody().uploadUrl());
            
            verify(storageService).initiateUploadProcess(testUserId, request);
        }
    }

    @Nested
    @DisplayName("completeUpload tests")
    class CompleteUploadTests {

        @Test
        @DisplayName("should return completed upload response")
        void shouldReturnCompletedUploadResponse() {
            CompleteUploadRequest request = new CompleteUploadRequest(12345L);
            
            CompleteUploadResponse expectedResponse = CompleteUploadResponse.builder()
                    .fileId("12345")
                    .filename("test.pdf")
                    .status(UploadStatus.VALIDATED)
                    .contentType("application/pdf")
                    .contentLength(2048L)
                    .build();
            
            when(storageService.verifyAndCompleteUpload(eq(testUserId), any(CompleteUploadRequest.class)))
                    .thenReturn(expectedResponse);
            
            CompleteUploadResponse result = storageController.completeUpload(request, authentication);
            
            assertNotNull(result);
            assertEquals("12345", result.fileId());
            assertEquals(UploadStatus.VALIDATED, result.status());
            
            verify(storageService).verifyAndCompleteUpload(testUserId, request);
        }
    }

    @Nested
    @DisplayName("serveFileValidatingAccess tests")
    class ServeFileTests {

        @Test
        @DisplayName("should return file stream for accessible file")
        void shouldReturnFileStreamForAccessibleFile() {
            StreamFileDto streamFileDto = StreamFileDto.builder()
                    .filename("test.png")
                    .contentType("image/png")
                    .contentLength(1024L)
                    .etag("abc123")
                    .inputStreamProvider(() -> new ByteArrayInputStream("test content".getBytes()))
                    .build();
            
            when(storageService.downloadFileValidatingAccess(eq(12345L), isNull()))
                    .thenReturn(streamFileDto);
            
            ResponseEntity<StreamingResponseBody> result = storageController.serveFileValidatingAccess(12345L, null);
            
            assertNotNull(result);
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertNotNull(result.getBody());
            assertNotNull(result.getHeaders().getFirst("Content-Disposition"));
            assertEquals("\"abc123\"", result.getHeaders().getETag());
        }

        @Test
        @DisplayName("should return 304 Not Modified when ETag matches")
        void shouldReturn304WhenETagMatches() {
            StreamFileDto streamFileDto = StreamFileDto.builder()
                    .etag("abc123")
                    .build();
            
            when(storageService.downloadFileValidatingAccess(eq(12345L), eq("abc123")))
                    .thenReturn(streamFileDto);
            
            ResponseEntity<StreamingResponseBody> result = storageController.serveFileValidatingAccess(12345L, "abc123");
            
            assertNotNull(result);
            assertEquals(HttpStatus.NOT_MODIFIED, result.getStatusCode());
        }

        @Test
        @DisplayName("should return 404 when service returns null")
        void shouldReturn404WhenServiceReturnsNull() {
            when(storageService.downloadFileValidatingAccess(eq(99999L), isNull()))
                    .thenReturn(null);
            
            ResponseEntity<StreamingResponseBody> result = storageController.serveFileValidatingAccess(99999L, null);
            
            assertNotNull(result);
            assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        }
    }

    @Nested
    @DisplayName("generateTempDownloadUrl tests")
    class GenerateTempUrlTests {

        @Test
        @DisplayName("should return presigned download URL")
        void shouldReturnPresignedDownloadUrl() {
            PreSignedDownloadUrlResponse expectedResponse = PreSignedDownloadUrlResponse.builder()
                    .url("https://storage.example.com/download/presigned")
                    .expiresAt(Instant.now().plusSeconds(180))
                    .build();
            
            when(storageService.generateTempDownloadUrl(eq(12345L), eq(testUserId)))
                    .thenReturn(expectedResponse);
            
            ResponseEntity<PreSignedDownloadUrlResponse> result = storageController.generateTempDownloadUrl(12345L, authentication);
            
            assertNotNull(result);
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals("https://storage.example.com/download/presigned", result.getBody().url());
        }
    }

    @Nested
    @DisplayName("deleteFile tests")
    class DeleteFileTests {

        @Test
        @DisplayName("should return 204 No Content when file deleted")
        void shouldReturn204WhenFileDeleted() {
            doNothing().when(storageService).deleteFile(eq(12345L), eq(testUserId));
            
            ResponseEntity<Void> result = storageController.deleteFile(12345L, authentication);
            
            assertNotNull(result);
            assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
            
            verify(storageService).deleteFile(12345L, testUserId);
        }
    }

    @Nested
    @DisplayName("getFileInfo tests")
    class GetFileInfoTests {

        @Test
        @DisplayName("should return file metadata")
        void shouldReturnFileMetadata() {
            FileUploadResponse expectedResponse = FileUploadResponse.builder()
                    .fileId("12345")
                    .ownerId(testUserId)
                    .filename("test.pdf")
                    .mimeType("application/pdf")
                    .contentSize(2048L)
                    .access(FileAccessLevel.PRIVATE)
                    .status(UploadStatus.VALIDATED)
                    .available(true)
                    .build();
            
            when(storageService.getFileInfo(12345L)).thenReturn(expectedResponse);
            
            FileUploadResponse result = storageController.getFileInfo(12345L);
            
            assertNotNull(result);
            assertEquals("12345", result.fileId());
            assertEquals("test.pdf", result.filename());
            assertEquals("application/pdf", result.mimeType());
        }
    }
}
