package com.swiftapp_backend.chatservice.chat.api.dto;

import com.swiftapp_backend.chatservice.chat.domain.enums.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    /** Client-generated idempotency key. */
    @NotBlank
    @Size(max = 128)
    private String messageId;

    @Size(max = 4000)
    private String messageContent;

    @Size(max = 2048)
    private String messageUrl;

    private ContentType contentType;

    /** Optional for the combined send endpoint; the reply endpoint supplies it in the path. */
    @Size(max = 128)
    private String replyOfMessageId;
}
