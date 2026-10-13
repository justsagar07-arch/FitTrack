package com.fittrack.app;

import com.fittrack.app.models.ChatMessage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChatMessageTest {

    @Test
    public void testUserChatMessage() {
        long now = System.currentTimeMillis();
        ChatMessage message = new ChatMessage("How to lose weight?", ChatMessage.TYPE_USER, now);

        assertEquals("How to lose weight?", message.getText());
        assertEquals(ChatMessage.TYPE_USER, message.getSenderType());
        assertEquals(now, message.getTimestamp());
    }

    @Test
    public void testAiChatMessage() {
        long now = System.currentTimeMillis();
        ChatMessage message = new ChatMessage("Focus on calories deficit and cardio.", ChatMessage.TYPE_AI, now);

        assertEquals("Focus on calories deficit and cardio.", message.getText());
        assertEquals(ChatMessage.TYPE_AI, message.getSenderType());
        assertEquals(now, message.getTimestamp());
    }
}
