package dev.fnvir.kajz.storageservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;

import dev.fnvir.kajz.storageservice.dto.UploadValidationResultDTO;
import dev.fnvir.kajz.storageservice.dto.UploadValidationResultDTO.UploadValidationFailureReason;
import dev.fnvir.kajz.storageservice.dto.req.CompleteUploadRequest;
import dev.fnvir.kajz.storageservice.dto.req.InitiateUploadRequest;
import dev.fnvir.kajz.storageservice.dto.res.CompleteUploadResponse;
import dev.fnvir.kajz.storageservice.dto.res.InitiateUploadResponse;
import dev.fnvir.kajz.storageservice.dto.res.PreSignedDownloadUrlResponse;
import dev.fnvir.kajz.storageservice.enums.FileAccessLevel;
import dev.fnvir.kajz.storageservice.enums.UploadStatus;
import dev.fnvir.kajz.storageservice.exception.ConflictException;
import dev.fnvir.kajz.storageservice.exception.ForbiddenException;
import dev.fnvir.kajz.storageservice.exception.NotFoundException;
import dev.fnvir.kajz.storageservice.mapper.FileUploadMapper;
import dev.fnvir.kajz.storageservice.model.FileUpload;
import dev.fnvir.kajz.storageservice.repository.StorageRepository;

/**
 * Unit tests for {@link StorageService}.
 */
@ExtendWith(MockitoExtension.class)
public class StorageServiceTest {

    @Mock
    private StorageRepository storageRepository;

    @Mock
    private AbstractStorageProvider storageProvider;

    @Spy
    private FileUploadMapper fileUploadMapper = FileUploadMapper.INSTANCE;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private StorageService storageService;

    private UUID testUserId;
    private FileUpload testFileUpload;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        testFileUpload = new FileUpload();
        testFileUpload.setId(12345L);
        testFileUpload.setOwnerId(testUserId);
        testFileUpload.setFilename("test-upload-abc123.png");
        testFileUpload.setStoragePath("public/encoded-uuid/test-upload-abc123.png");
        testFileUpload.setMimeType("image/png");
        testFileUpload.setContentSize(1024L);
        testFileUpload.setAccess(FileAccessLevel.PUBLIC);
        testFileUpload.setStatus(UploadStatus.UPLOADING);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
    
    @Nested
    @DisplayName("initiateUploadProcess tests")
    class InitiateUploadProcessTests {

        @Test
        @DisplayName("should create file upload and return presigned URL")
        void shouldCreateFileUploadAndReturnPresignedUrl() {
            InitiateUploadRequest request = new InitiateUploadRequest(
                    "document.pdf",
                    "application/pdf",
                    2048L,
                    FileAccessLevel.PRIVATE,
                    "user-document"
            );
            
            Long mockFileId = 123456L;
            
            InitiateUploadResponse expectedResponse = InitiateUploadResponse.builder()
                    .fileId(mockFileId)
                    .uploadUrl("https://storage.example.com/presigned-upload-url")
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();
            
            when(storageRepository.saveAndFlush(any(FileUpload.class))).thenAnswer(invocation -> {
                FileUpload file = invocation.getArgument(0);
                file.setId(mockFileId);
                return file;
            });
            
            when(storageProvider.initiateUpload(any(FileUpload.class))).thenReturn(expectedResponse);
            
            InitiateUploadResponse result = storageService.initiateUploadProcess(testUserId, request);
            
            assertNotNull(result);
            assertEquals(expectedResponse.fileId(), result.fileId());
            assertEquals(expectedResponse.uploadUrl(), result.uploadUrl());
            
            ArgumentCaptor<FileUpload> fileCaptor = ArgumentCaptor.forClass(FileUpload.class);
            verify(storageRepository).saveAndFlush(fileCaptor.capture());
            
            FileUpload savedFile = fileCaptor.getValue();
            assertEquals(testUserId, savedFile.getOwnerId());
            assertEquals("application/pdf", savedFile.getMimeType());
            assertEquals(2048L, savedFile.getContentSize());
            assertEquals(FileAccessLevel.PRIVATE, savedFile.getAccess());
            assertEquals(UploadStatus.UPLOADING, savedFile.getStatus());
            assertTrue(savedFile.getFilename().startsWith("user-document-"));
            assertTrue(savedFile.getFilename().endsWith(".pdf"));
        }

        @Test
        @DisplayName("should use default extension when filename has no extension")
        void shouldUseDefaultExtensionWhenMissing() {
            InitiateUploadRequest request = new InitiateUploadRequest(
                    "noextension",
                    "application/octet-stream",
                    1024L,
                    FileAccessLevel.PUBLIC,
                    null
            );
            
            when(storageRepository.saveAndFlush(any(FileUpload.class))).thenAnswer(invocation -> {
                FileUpload file = invocation.getArgument(0);
                file.setId(1L);
                return file;
            });
            when(storageProvider.initiateUpload(any())).thenReturn(
                    InitiateUploadResponse.builder().fileId(1L).uploadUrl("url").build()
            );
            
            storageService.initiateUploadProcess(testUserId, request);
            
            ArgumentCaptor<FileUpload> fileCaptor = ArgumentCaptor.forClass(FileUpload.class);
            verify(storageRepository).saveAndFlush(fileCaptor.capture());
            
            assertTrue(fileCaptor.getValue().getFilename().endsWith(".bin"));
        }

        @Test
        @DisplayName("should generate correct storage path for private files")
        void shouldGenerateCorrectStoragePathForPrivateFiles() {
            InitiateUploadRequest request = new InitiateUploadRequest(
                    "secret.pdf",
                    "application/pdf",
                    1024L,
                    FileAccessLevel.PRIVATE,
                    "docs"
            );
            
            when(storageRepository.saveAndFlush(any(FileUpload.class))).thenAnswer(invocation -> {
                FileUpload file = invocation.getArgument(0);
                file.setId(1L);
                return file;
            });
            when(storageProvider.initiateUpload(any())).thenReturn(
                    InitiateUploadResponse.builder().fileId(1L).uploadUrl("url").build()
            );
            
            storageService.initiateUploadProcess(testUserId, request);
            
            ArgumentCaptor<FileUpload> fileCaptor = ArgumentCaptor.forClass(FileUpload.class);
            verify(storageRepository).saveAndFlush(fileCaptor.capture());
            
            String storagePath = fileCaptor.getValue().getStoragePath();
            assertTrue(storagePath.startsWith("private/"));
        }

        @Test
        @DisplayName("should generate correct storage path for public files")
        void shouldGenerateCorrectStoragePathForPublicFiles() {
            InitiateUploadRequest request = new InitiateUploadRequest(
                    "image.png",
                    "image/png",
                    1024L,
                    FileAccessLevel.PUBLIC,
                    "profile"
            );
            
            when(storageRepository.saveAndFlush(any(FileUpload.class))).thenAnswer(invocation -> {
                FileUpload file = invocation.getArgument(0);
                file.setId(1L);
                return file;
            });
            when(storageProvider.initiateUpload(any())).thenReturn(
                    InitiateUploadResponse.builder().fileId(1L).uploadUrl("url").build()
            );
            
            storageService.initiateUploadProcess(testUserId, request);
            
            ArgumentCaptor<FileUpload> fileCaptor = ArgumentCaptor.forClass(FileUpload.class);
            verify(storageRepository).saveAndFlush(fileCaptor.capture());
            
            String storagePath = fileCaptor.getValue().getStoragePath();
            assertTrue(storagePath.startsWith("public/"));
        }
    }

    @Nested
    @DisplayName("verifyAndCompleteUpload tests")
    class VerifyAndCompleteUploadTests {
        
        @Captor
        private ArgumentCaptor<FileUpload> fileUploadCaptor;

        @Test
        @DisplayName("should complete upload when validation succeeds")
        void shouldCompleteUploadWhenValidationSucceeds() {
            testFileUpload.setStatus(UploadStatus.UPLOADING);
            testFileUpload.setCompletedAt(null);
            
            CompleteUploadRequest request = new CompleteUploadRequest(testFileUpload.getId());
            
            UploadValidationResultDTO validationResult = new UploadValidationResultDTO();
            validationResult.setSuccess(true);
            validationResult.setETag("abc123etag");
            
            when(storageRepository.findById(eq(testFileUpload.getId()))).thenReturn(Optional.of(testFileUpload));
            when(storageProvider.validateUploadCompletion(testFileUpload)).thenReturn(validationResult);
            when(storageRepository.save(any(FileUpload.class))).thenAnswer(inv -> inv.getArgument(0));
            
            CompleteUploadResponse result = storageService.verifyAndCompleteUpload(testUserId, request);
            
            verify(fileUploadMapper).toUploadCompleteResponse(fileUploadCaptor.capture());
            
            var capturedValue = fileUploadCaptor.getValue();
            assertEquals(capturedValue.getStatus(), UploadStatus.VALIDATED);
            assertNotNull(capturedValue.getCompletedAt());
            
            assertNotNull(result);
            assertEquals(UploadStatus.VALIDATED, result.status());
            assertNotNull(result.completedAt());
            assertEquals("abc123etag", testFileUpload.getETag());
        }

        @Test
        @DisplayName("should throw NotFoundException when file not found")
        void shouldThrowNotFoundWhenFileNotFound() {
            CompleteUploadRequest request = new CompleteUploadRequest(99999L);
            
            when(storageRepository.findById(99999L)).thenReturn(Optional.empty());
            
            assertThrows(NotFoundException.class,
                    () -> storageService.verifyAndCompleteUpload(testUserId, request));
        }

        @Test
        @DisplayName("should throw ForbiddenException when user is not owner")
        void shouldThrowForbiddenWhenNotOwner() {
            CompleteUploadRequest request = new CompleteUploadRequest(12345L);
            testFileUpload.setOwnerId(UUID.randomUUID());
            UUID authUserId = UUID.randomUUID();
            
            when(storageRepository.findById(12345L)).thenReturn(Optional.of(testFileUpload));
            
            assertThrows(ForbiddenException.class,
                    () -> storageService.verifyAndCompleteUpload(authUserId, request));
        }

        @Test
        @DisplayName("should throw ConflictException when already completed")
        void shouldThrowConflictWhenAlreadyCompleted() {
            CompleteUploadRequest request = new CompleteUploadRequest(12345L);
            testFileUpload.setStatus(UploadStatus.VALIDATED);
            testFileUpload.setCompletedAt(Instant.now());
            
            when(storageRepository.findById(12345L)).thenReturn(Optional.of(testFileUpload));
            
            assertThrows(ConflictException.class,
                    () -> storageService.verifyAndCompleteUpload(testUserId, request));
        }

        @Test
        @DisplayName("should throw NotFoundException when file doesn't exist on storage")
        void shouldThrowNotFoundWhenFileDoesntExistOnStorage() {
            CompleteUploadRequest request = new CompleteUploadRequest(12345L);
            testFileUpload.setStatus(UploadStatus.UPLOADING);
            testFileUpload.setCompletedAt(null);
            
            UploadValidationResultDTO validationResult = new UploadValidationResultDTO();
            validationResult.setSuccess(false);
            validationResult.setFailureReason(UploadValidationFailureReason.FILE_DOESNT_EXIST);
            validationResult.setMessage("File not found on storage");
            
            when(storageRepository.findById(12345L)).thenReturn(Optional.of(testFileUpload));
            when(storageProvider.validateUploadCompletion(testFileUpload)).thenReturn(validationResult);
            
            assertThrows(NotFoundException.class,
                    () -> storageService.verifyAndCompleteUpload(testUserId, request));
        }
    }

    @Nested
    @DisplayName("generateTempDownloadUrl tests")
    class GenerateTempDownloadUrlTests {

        @Test
        @DisplayName("should generate presigned URL for owner")
        void shouldGeneratePresignedUrlForOwner() {
            testFileUpload.setStatus(UploadStatus.VALIDATED);
            testFileUpload.setCompletedAt(Instant.now());
            testFileUpload.setDeleted(false);
            
            PreSignedDownloadUrlResponse expectedResponse = PreSignedDownloadUrlResponse.builder()
                    .url("https://storage.example.com/presigned-download")
                    .expiresAt(Instant.now().plusSeconds(180))
                    .build();
            
            when(storageRepository.findById(12345L)).thenReturn(Optional.of(testFileUpload));
            when(storageProvider.generatePreSignedDownloadUrl(anyString(), any(Duration.class)))
                    .thenReturn(expectedResponse);
            
            PreSignedDownloadUrlResponse result = storageService.generateTempDownloadUrl(12345L, testUserId);
            
            assertNotNull(result);
            assertEquals(expectedResponse.url(), result.url());
            verify(storageProvider).generatePreSignedDownloadUrl(eq(testFileUpload.getStoragePath()), any(Duration.class));
        }

        @Test
        @DisplayName("should throw NotFoundException when file not available")
        void shouldThrowNotFoundWhenFileNotAvailable() {
            testFileUpload.setStatus(UploadStatus.UPLOADING);
            testFileUpload.setCompletedAt(null);
            
            when(storageRepository.findById(12345L)).thenReturn(Optional.of(testFileUpload));
            
            assertThrows(NotFoundException.class,
                    () -> storageService.generateTempDownloadUrl(12345L, testUserId));
        }

        @Test
        @DisplayName("should throw NotFoundException when file is deleted")
        void shouldThrowNotFoundWhenFileDeleted() {
            testFileUpload.setStatus(UploadStatus.VALIDATED);
            testFileUpload.setCompletedAt(Instant.now());
            testFileUpload.setDeleted(true);
            
            when(storageRepository.findById(12345L)).thenReturn(Optional.of(testFileUpload));
            
            assertThrows(NotFoundException.class,
                    () -> storageService.generateTempDownloadUrl(12345L, testUserId));
        }
    }
}
