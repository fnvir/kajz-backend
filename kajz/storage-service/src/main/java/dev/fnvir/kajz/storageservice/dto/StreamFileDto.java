package dev.fnvir.kajz.storageservice.dto;

import java.io.InputStream;
import java.util.concurrent.Callable;

import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StreamFileDto {
    
    private Callable<InputStream> inputStreamProvider;
    private String filename;
    private long contentLength;
    private String contentType;
    private String etag;
    
    public MediaType getMediaType() {
        if (contentType != null && !contentType.equals("application/octet-stream")) {
            try { return MediaType.valueOf(contentType); }
            catch (Exception _) {}
        }
        return MediaTypeFactory.getMediaType(filename)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }
    
    public StreamingResponseBody streamFile() {
        return outStream -> {
            try (InputStream inputStream = inputStreamProvider.call()) {
                if (inputStream == null) {
                    return;
                }
                inputStream.transferTo(outStream);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

}
