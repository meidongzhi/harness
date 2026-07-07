package com.example.companion;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class ChatController {
    private final List<Map<String, String>> history = new ArrayList<>();

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        String reply = generateReply(message);
        history.add(Map.of("role", "user", "content", message));
        history.add(Map.of("role", "assistant", "content", reply));
        return Map.of("response", reply);
    }

    @GetMapping("/history")
    public List<Map<String, String>> history() {
        return history;
    }

    private String generateReply(String msg) {
        String lower = msg.toLowerCase();
        if (lower.contains("hello") || lower.contains("hi") || lower.contains("你好")) {
            return "Hello! 很高兴见到你~ 今天想聊点什么？";
        }
        if (lower.contains("name") || lower.contains("名字")) {
            return "我叫小伴，是你的AI聊天伙伴！";
        }
        if (lower.contains("weather") || lower.contains("天气")) {
            return "天气不错呢！适合出去走走~";
        }
        if (lower.contains("bye") || lower.contains("再见")) {
            return "再见！我会想你的~ 随时回来找我聊天哦！";
        }
        return "嗯嗯，我在听~ 继续说吧！";
    }
}
