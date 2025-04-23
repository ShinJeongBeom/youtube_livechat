package com.example.youtube_livechat.service;

import com.example.youtube_livechat.model.ChatComment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    public List<ChatComment> getUserComments(String videoId, String targetUsername) throws Exception {
        String outputFileName = videoId + ".live_chat.json";
        File chatFile = new File(outputFileName);
        if (!chatFile.exists()) throw new FileNotFoundException("채팅 JSON 파일을 찾을 수 없습니다.");

        ObjectMapper mapper = new ObjectMapper();
        List<ChatComment> result = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(chatFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    JsonNode node = mapper.readTree(line);
                    JsonNode replayAction = node.get("replayChatItemAction");
                    if (replayAction == null) continue;

                    for (JsonNode action : replayAction.get("actions")) {
                        JsonNode addChatItemAction = action.get("addChatItemAction");
                        if (addChatItemAction == null) continue;

                        JsonNode item = addChatItemAction.get("item");
                        if (item == null || !item.has("liveChatTextMessageRenderer")) continue;

                        JsonNode chat = item.get("liveChatTextMessageRenderer");

                        String author = chat.path("authorName").path("simpleText").asText().trim();
                        System.out.println("채팅 작성자: " + author);

                        // 필터링 조건 없으면 전체 유저
                        if (targetUsername != null && !targetUsername.isBlank()) {
                            if (!author.replaceAll("\\s", "").contains(targetUsername.replaceAll("\\s", ""))) continue;
                        }

                        // 메시지 조합 (emoji + text)
                        StringBuilder messageBuilder = new StringBuilder();
                        for (JsonNode run : chat.path("message").path("runs")) {
                            if (run.has("text")) {
                                messageBuilder.append(run.path("text").asText());
                            } else if (run.has("emoji")) {
                                JsonNode shortcuts = run.path("emoji").path("shortcuts");
                                if (shortcuts.isArray() && shortcuts.size() > 0) {
                                    messageBuilder.append(shortcuts.get(0).asText());
                                }
                            }
                        }

                        String message = messageBuilder.toString();
                        String timestamp = chat.path("timestampUsec").asText();

                        result.add(new ChatComment(author, message, timestamp));
                    }

                } catch (Exception e) {
                    // JSON 파싱 실패 무시
                }
            }
        }

        System.out.println("필터링된 채팅 수: " + result.size());
        return result;
    }


    public void saveCommentsToExcel(List<ChatComment> comments, String videoId, String username) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Filtered Chat");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("작성자");
        header.createCell(1).setCellValue("내용");
        header.createCell(2).setCellValue("타임스탬프");

        for (int i = 0; i < comments.size(); i++) {
            ChatComment comment = comments.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(comment.getAuthor());
            row.createCell(1).setCellValue(comment.getMessage());
            row.createCell(2).setCellValue(comment.getTime());
        }

        String filename = videoId + "_" + username + "_filtered_chat.xlsx";
        try (FileOutputStream fileOut = new FileOutputStream(filename)) {
            workbook.write(fileOut);
        }

        workbook.close();
    }
}
