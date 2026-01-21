package dev.fnvir.kajz.authservice.dto.event;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for sending an email.
 */
@Data
public class EmailEvent {

    /** The recipient email addresses. */
    @Valid
    @NotEmpty
    private Set<@Email String> to;

    /** The CC email addresses. */
    @Valid
    @Nullable
    private Set<@Email String> cc;

    /** The BCC email addresses. */
    @Valid
    @Nullable
    private Set<@Email String> bcc;

    /** The subject of the email. */
    @NotBlank(message = "Subject is required")
    @Size(max = 200)
    private String subject;

    /** The body content of the email. */
    @NotBlank(message = "Body is required")
    private String content;

    /**
     * Whether the content is an HTML. Default is true.
     */
    private boolean isHtml = true;

    /**
     * The priority of the email (1 = highest, 5 = lowest). Default is 3.
     */
    @Min(1)
    @Max(5)
    @Schema(hidden = true)
    private int priority = 3;

}

