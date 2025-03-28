package com.example.demo.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class ChatController {

    @MessageMapping("/send")
    @SendTo("/topic/messages")
    public Map<String, Object> handleMessage(Map<String, Object> message) {
        message.put("timestamp", LocalDateTime.now().toString());
        message.put("isFromCurrentUser", false); // Флаг дали съобщението е от текущия потребител

        System.out.println("Forwarding message: " + message);
        return message;
    }
}