package dev.fnvir.kajz.storageservice.dto;

import java.io.InputStream;
import java.util.concurrent.Callable;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Flux;

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
    
    public Flux<DataBuffer> streamFile(DataBufferFactory bufferFactory) {
        return Flux.defer(() -> {
            return DataBufferUtils.readInputStream(inputStreamProvider, bufferFactory, 16 * 1024);
        });
    }

}
