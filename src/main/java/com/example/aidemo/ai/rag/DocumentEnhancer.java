package com.example.aidemo.ai.rag;

import dev.langchain4j.data.document.Document;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档增强处理器 - 处理Markdown文档中的图片
 */
@Slf4j
@Component
public class DocumentEnhancer {

    @Resource
    private ImageRecognitionService imageRecognitionService;

    // 匹配Markdown图片语法: ![alt](path) 或 <img src="path">
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile(
            "!\\[([^\\]]*)\\]\\(([^\\)]+)\\)"
    );
    
    private static final Pattern HTML_IMAGE_PATTERN = Pattern.compile(
            "<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>"
    );

    /**
     * 增强文档：识别图片并替换为文本描述
     * 
     * @param document 原始文档
     * @param docDir 文档目录（用于解析相对路径）
     * @return 增强后的文档
     */
    public Document enhanceDocument(Document document, String docDir) {
        String content = document.text();
        String fileName = document.metadata().getString("file_name");
        
        log.info("开始增强文档: {}", fileName);
        
        // 检查是否是Markdown文件
        if (fileName == null || (!fileName.endsWith(".md") && !fileName.endsWith(".markdown"))) {
            log.debug("非Markdown文件，跳过图片处理: {}", fileName);
            return document;
        }
        
        // 提取所有图片路径
        List<String> imagePaths = extractImagePaths(content, docDir);
        
        if (imagePaths.isEmpty()) {
            log.debug("文档中没有图片: {}", fileName);
            return document;
        }
        
        log.info("文档 {} 包含 {} 张图片，开始识别...", fileName, imagePaths.size());
        
        // 识别所有图片
        Map<String, String> imageDescriptions = imageRecognitionService.describeImages(imagePaths);
        
        // 替换文档中的图片为描述文本
        String enhancedContent = replaceImagesWithDescriptions(content, imageDescriptions);
        
        // 创建新的文档（保留原始metadata）
        Document enhancedDoc = Document.from(enhancedContent, document.metadata());
        
        log.info("文档增强完成: {}", fileName);
        return enhancedDoc;
    }

    /**
     * 从文档内容中提取所有图片路径
     */
    private List<String> extractImagePaths(String content, String docDir) {
        List<String> imagePaths = new ArrayList<>();
        
        // 提取Markdown格式的图片
        Matcher markdownMatcher = MARKDOWN_IMAGE_PATTERN.matcher(content);
        while (markdownMatcher.find()) {
            String imagePath = markdownMatcher.group(2);
            String fullPath = resolveImagePath(imagePath, docDir);
            if (fullPath != null) {
                imagePaths.add(fullPath);
            }
        }
        
        // 提取HTML格式的图片
        Matcher htmlMatcher = HTML_IMAGE_PATTERN.matcher(content);
        while (htmlMatcher.find()) {
            String imagePath = htmlMatcher.group(1);
            String fullPath = resolveImagePath(imagePath, docDir);
            if (fullPath != null) {
                imagePaths.add(fullPath);
            }
        }
        
        return imagePaths;
    }

    /**
     * 解析图片路径（处理相对路径）
     */
    private String resolveImagePath(String imagePath, String docDir) {
        try {
            // 如果是绝对路径，直接返回
            if (Paths.get(imagePath).isAbsolute()) {
                File file = new File(imagePath);
                return file.exists() ? imagePath : null;
            }
            
            // 如果是相对路径，基于文档目录解析
            Path docPath = Paths.get(docDir);
            Path resolvedPath = docPath.resolve(imagePath).normalize();
            
            File file = resolvedPath.toFile();
            if (file.exists()) {
                return file.getAbsolutePath();
            }
            
            log.warn("图片文件不存在: {}", imagePath);
            return null;
            
        } catch (Exception e) {
            log.warn("解析图片路径失败: {}", imagePath, e);
            return null;
        }
    }

    /**
     * 将文档中的图片替换为描述文本
     */
    private String replaceImagesWithDescriptions(String content, Map<String, String> imageDescriptions) {
        String enhancedContent = content;
        
        // 替换Markdown格式的图片
        Matcher markdownMatcher = MARKDOWN_IMAGE_PATTERN.matcher(enhancedContent);
        StringBuffer sb = new StringBuffer();
        
        while (markdownMatcher.find()) {
            String altText = markdownMatcher.group(1);
            String imagePath = markdownMatcher.group(2);
            
            // 查找对应的描述
            String description = findImageDescription(imagePath, imageDescriptions);
            
            // 替换为：原文 + 图片描述
            String replacement = altText + "\n\n" + description + "\n";
            markdownMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        markdownMatcher.appendTail(sb);
        enhancedContent = sb.toString();
        
        // 替换HTML格式的图片
        Matcher htmlMatcher = HTML_IMAGE_PATTERN.matcher(enhancedContent);
        sb = new StringBuffer();
        
        while (htmlMatcher.find()) {
            String imgTag = htmlMatcher.group(0);
            String imagePath = htmlMatcher.group(1);
            
            // 查找对应的描述
            String description = findImageDescription(imagePath, imageDescriptions);
            
            // 替换为描述文本
            String replacement = "\n" + description + "\n";
            htmlMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        htmlMatcher.appendTail(sb);
        enhancedContent = sb.toString();
        
        return enhancedContent;
    }

    /**
     * 查找图片的描述
     */
    private String findImageDescription(String imagePath, Map<String, String> imageDescriptions) {
        // 尝试精确匹配
        if (imageDescriptions.containsKey(imagePath)) {
            return imageDescriptions.get(imagePath);
        }
        
        // 尝试文件名匹配
        String fileName = Paths.get(imagePath).getFileName().toString();
        for (Map.Entry<String, String> entry : imageDescriptions.entrySet()) {
            if (entry.getKey().endsWith(fileName)) {
                return entry.getValue();
            }
        }
        
        return "[图片描述不可用]";
    }

    /**
     * 批量增强文档
     */
    public List<Document> enhanceDocuments(List<Document> documents, String docDir) {
        List<Document> enhancedDocs = new ArrayList<>();
        
        for (Document doc : documents) {
            try {
                Document enhancedDoc = enhanceDocument(doc, docDir);
                enhancedDocs.add(enhancedDoc);
            } catch (Exception e) {
                log.error("文档增强失败: {}", doc.metadata().getString("file_name"), e);
                // 失败时使用原文档
                enhancedDocs.add(doc);
            }
        }
        
        return enhancedDocs;
    }
}
