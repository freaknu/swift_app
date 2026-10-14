package com.swiftapp_backend.websocketservice.realtime.infrastructure.config;

import com.swiftapp_backend.websocketservice.realtime.api.dto.ChatMessageDto;
import com.swiftapp_backend.websocketservice.realtime.domain.enums.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessagingException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class ChatMessageWriteClientTest {
    private MockRestServiceServer server;
    private ChatMessageWriteClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://chat");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ChatMessageWriteClient(builder.build());
    }

    @Test
    void sendsOnlyWritableFieldsWithAuthenticatedAccountIdentity() {
        server.expect(requestTo("http://chat/api/v1/chat/sessions/7/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-user-id", "42"))
                .andExpect(jsonPath("$.messageId").value("m-1"))
                .andExpect(jsonPath("$.contentType").value("TEXT"))
                .andExpect(jsonPath("$.sentById").doesNotExist())
                .andExpect(jsonPath("$.readByIds").doesNotExist())
                .andExpect(jsonPath("$.deleted").doesNotExist())
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{}"));

        client.send(42L, ChatMessageDto.builder().messageId("m-1")
                .sentById(999L).sessionId(7L).messageContent("hello")
                .contentType(ContentType.TEXT).readByIds("999").deleted(true).build(), false);

        server.verify();
    }

    @Test
    void replyPreservesItsParentOnTheDurableWritePath() {
        server.expect(requestTo("http://chat/api/v1/chat/sessions/7/messages"))
                .andExpect(jsonPath("$.replyOfMessageId").value("parent"))
                .andRespond(withStatus(HttpStatus.CREATED));

        client.send(42L, ChatMessageDto.builder().messageId("reply")
                .sessionId(7L).messageContent("reply").replyOfMessageId("parent").build(), true);

        server.verify();
    }

    @Test
    void outsiderAndInactiveSessionResponsesNeverFallBackToLegacyPublishing() {
        server.expect(requestTo("http://chat/api/v1/chat/sessions/7/messages"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        server.expect(requestTo("http://chat/api/v1/chat/sessions/7/messages"))
                .andRespond(withStatus(HttpStatus.CONFLICT));
        ChatMessageDto message = ChatMessageDto.builder().messageId("m-1")
                .sessionId(7L).messageContent("hello").build();

        assertThrows(MessagingException.class, () -> client.send(42L, message, false));
        assertThrows(MessagingException.class, () -> client.send(42L, message, false));

        server.verify();
    }

    @Test
    void missingAuthenticatedIdentityOrReplyTargetIsRejectedBeforeHttp() {
        ChatMessageDto message = ChatMessageDto.builder().messageId("m-1")
                .sessionId(7L).messageContent("hello").build();
        assertThrows(MessagingException.class, () -> client.send(null, message, false));
        assertThrows(MessagingException.class, () -> client.send(42L, message, true));
        server.verify();
    }
}
