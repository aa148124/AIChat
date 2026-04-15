package com.example.aidemo.ai.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Base64;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图片识别服务 - 使用通义千问VL模型识别图片内容
 */
@Slf4j
@Service
public class ImageRecognitionService {

    @Value("${langchain4j.community.dashscope.chat-model.api-key:}")
    private String apiKey;

    private static final String VISION_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    /**
     * 识别图片内容并返回文本描述
     * 
     * @param imagePath 图片路径
     * @return 图片的文本描述
     */
    public String describeImage(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                log.warn("图片文件不存在: {}", imagePath);
                return "[图片不存在]";
            }

            // 读取图片并转换为Base64
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            
            // 获取图片格式
            String fileName = imageFile.getName().toLowerCase();
            String mimeType = "image/jpeg";
            if (fileName.endsWith(".png")) {
                mimeType = "image/png";
            } else if (fileName.endsWith(".gif")) {
                mimeType = "image/gif";
            } else if (fileName.endsWith(".webp")) {
                mimeType = "image/webp";
            }

            // 调用视觉模型识别图片
            String prompt = "请详细描述这张图片的内容，包括：\n" +
                    "1. 图片中显示的主要内容\n" +
                    "2. 如果有文字，请提取文字内容\n" +
                    "3. 如果有图表、流程图或界面截图，请说明其含义\n" +
                    "4. 图片的关键信息和要点\n" +
                    "\n请以简洁清晰的文本形式描述，不超过200字。";

            log.info("正在识别图片: {}", imagePath);
            String description = callVisionModel(prompt, base64Image, mimeType);
            log.info("图片识别完成: {}", description);

            return "[图片描述] " + description;

        } catch (Exception e) {
            log.error("图片识别失败: {}", imagePath, e);
            return "[图片识别失败: " + e.getMessage() + "]";
        }
    }

    /**
     * 调用通义千问视觉模型
     */
    private String callVisionModel(String prompt, String base64Image, String mimeType) {
        RestTemplate restTemplate = new RestTemplate();
        
        // 构造请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "qwen-vl-max");
        
        // 构造消息
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        
        // 构造内容（文本+图片）
        List<Map<String, Object>> content = new ArrayList<>();
        
        // 文本部分
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", prompt);
        content.add(textPart);
        
        // 图片部分
        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        Map<String, String> imageUrl = new HashMap<>();
        imageUrl.put("url", "data:" + mimeType + ";base64," + base64Image);
        imagePart.put("image_url", imageUrl);
        content.add(imagePart);
        
        message.put("content", content);
        messages.add(message);
        requestBody.put("messages", messages);
        
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        // 发送请求
        ResponseEntity<Map> response = restTemplate.postForEntity(VISION_API_URL, entity, Map.class);
        
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                Map<String, Object> messageResult = (Map<String, Object>) choice.get("message");
                return (String) messageResult.get("content");
            }
        }
        
        throw new RuntimeException("视觉模型调用失败");
    }

    /**
     * 批量识别图片
     * 
     * @param imagePaths 图片路径列表
     * @return 图片描述映射 (路径 -> 描述)
     */
    public Map<String, String> describeImages(List<String> imagePaths) {
        Map<String, String> descriptions = new HashMap<>();
        
        for (String path : imagePaths) {
            String description = describeImage(path);
            descriptions.put(path, description);
        }
        
        return descriptions;
    }
}
