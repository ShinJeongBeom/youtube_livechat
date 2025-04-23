package com.example.youtube_livechat.controller;

import com.example.youtube_livechat.model.ChatComment;
import com.example.youtube_livechat.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/filter")
    public List<ChatComment> getUserComments(
            @RequestParam String videoId,
            @RequestParam(required = false) String username
    ) throws Exception {
        return chatService.getUserComments(videoId, username);
    }



    @GetMapping("/filter/excel")
    public ResponseEntity<String> saveFilteredChatToExcel(
            @RequestParam String videoId,
            @RequestParam(required = false) String username // ✅ 옵션 처리
    ) {
        try {
            List<ChatComment> comments = chatService.getUserComments(videoId, username);
            chatService.saveCommentsToExcel(comments, videoId, (username != null ? username : "all"));
            return ResponseEntity.ok("엑셀 파일로 저장 완료: " + videoId + "_" + (username != null ? username : "all") + "_filtered_chat.xlsx");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("엑셀 저장 중 오류: " + e.getMessage());
        }
    }


}
