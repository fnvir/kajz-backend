package dev.fnvir.kajz.storageservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UploadValidationResultDTO {
    
    /** Whether the validation failed or succeeded. */
    private boolean success;
    
    /** The message of the validation failure. */
    private String message;
    
    /** The reason of the validation failure. */
    private UploadValidationFailureReason failureReason;
    
    /** The ETag of the uploaded file. */
    private String eTag;
    
    
    private UploadValidationResultDTO(boolean success) {
        this.success = success;
    }
    
    public static UploadValidationResultDTO success() {
        return new UploadValidationResultDTO(true);
    }
    
    public static UploadValidationResultDTO failed() {
        return new UploadValidationResultDTO(false);
    }
    
    public static UploadValidationResultDTO fileDoesntExist() {
        return failed().reason(UploadValidationFailureReason.FILE_DOESNT_EXIST)
                .message("File not uploaded or doesn't exist!");
    }
    
    public static UploadValidationResultDTO invalidContentLength() {
        return failed().reason(UploadValidationFailureReason.INVALID_CONTENT_LENGTH)
                .message("Uploaded file's size is larger than max allowed size!");
    }
    
    public static UploadValidationResultDTO invalidContentType() {
        return failed().reason(UploadValidationFailureReason.INVALID_CONTENT_TYPE)
                .message("Uploaded file's content-type isn't allowed!");
    }
    
    public UploadValidationResultDTO message(String message) {
        this.message = message;
        return this;
    }
    
    public UploadValidationResultDTO reason(UploadValidationFailureReason failureReason) {
        this.failureReason = failureReason;
        return this;
    }
    
    public UploadValidationResultDTO eTag(String eTag) {
        this.eTag = eTag;
        return this;
    }
    
    
    public enum UploadValidationFailureReason {
        FILE_DOESNT_EXIST,
        INVALID_CONTENT_LENGTH,
        INVALID_CONTENT_TYPE,
    }

}
