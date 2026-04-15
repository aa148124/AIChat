package com.example.aidemo.ai.config.rag;

import com.example.aidemo.ai.rag.DocumentEnhancer;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Configuration
public class RagConfig {

    private static final String DOC_DIR = "src/main/resources/doc";
    private static final String HASH_FILE = ".rag_doc_hash";
    private static final String COLLECTION_NAME = "rag_collection";

    @Resource
    private EmbeddingModel qwenEmbeddingModel;
    
    @Resource
    private DocumentEnhancer documentEnhancer;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return MilvusEmbeddingStore.builder()
                .host("localhost")
                .port(19530)
                .dimension(1024)
                .collectionName(COLLECTION_NAME)
                .build();
    }
    
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore) {
        log.info("初始化RAG内容检索器...");
        
        // 检查是否需要重新向量化
        if (needReIndex()) {
            log.info("检测到文档变更或首次初始化，开始向量化...");
            reIndexDocuments(embeddingStore);
            saveCurrentHash();
            log.info("文档向量化完成！");
        } else {
            log.info("文档未变更，跳过向量化，使用已有向量数据。");
        }
        
        //构造内容检索器
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(5)
                .build();
    }

    /**
     * 检查是否需要重新向量化
     */
    private boolean needReIndex() {
        try {
            // 检查哈希文件是否存在（存储在项目根目录）
            Path hashFilePath = Paths.get(HASH_FILE);
            if (!Files.exists(hashFilePath)) {
                log.info("未找到哈希记录文件，需要首次向量化");
                return true;
            }

            // 计算当前文档的哈希值
            String currentHash = calculateDocDirectoryHash();
            
            // 读取上次保存的哈希值
            String savedHash = Files.readString(hashFilePath).trim();
            
            // 比较哈希值
            boolean needReIndex = !currentHash.equals(savedHash);
            if (needReIndex) {
                log.info("文档已变更，需要重新向量化");
            }
            return needReIndex;
        } catch (Exception e) {
            log.warn("检查文档哈希失败，将重新向量化: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 计算文档目录的哈希值（基于文件名和修改时间）
     */
    private String calculateDocDirectoryHash() throws Exception {
        Path docPath = Paths.get(DOC_DIR);
        StringBuilder sb = new StringBuilder();
        
        try (Stream<Path> walk = Files.walk(docPath)) {
            List<String> fileInfos = walk
                    .filter(Files::isRegularFile)
                    // 排除哈希文件本身
                    .filter(path -> !path.getFileName().toString().equals(HASH_FILE))
                    .map(path -> {
                        try {
                            String fileName = path.getFileName().toString();
                            long lastModified = Files.getLastModifiedTime(path).toMillis();
                            return fileName + "|" + lastModified;
                        } catch (Exception e) {
                            return "";
                        }
                    })
                    .filter(s -> !s.isEmpty())
                    .sorted()
                    .collect(Collectors.toList());
            
            for (String info : fileInfos) {
                sb.append(info);
            }
        }
        
        // 使用MD5生成哈希
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hashBytes = md.digest(sb.toString().getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    /**
     * 保存当前文档的哈希值（存储在项目根目录）
     */
    private void saveCurrentHash() {
        try {
            String currentHash = calculateDocDirectoryHash();
            Path hashFilePath = Paths.get(HASH_FILE);
            Files.writeString(hashFilePath, currentHash);
            log.info("已保存文档哈希值到项目根目录: {}", currentHash);
        } catch (Exception e) {
            log.warn("保存文档哈希值失败: {}", e.getMessage());
        }
    }

    /**
     * 执行文档向量化
     */
    private void reIndexDocuments(EmbeddingStore<TextSegment> embeddingStore) {
        // 1.加载文档
        List<Document> docs = FileSystemDocumentLoader.loadDocuments(DOC_DIR);
        log.info("加载了 {} 个文档", docs.size());
        
        if (docs.isEmpty()) {
            log.warn("文档目录为空，跳过向量化");
            return;
        }
        
        // 2.增强文档（识别图片并转换为文本描述）
        log.info("开始增强文档（处理图片）...");
        List<Document> enhancedDocs = documentEnhancer.enhanceDocuments(docs, DOC_DIR);
        log.info("文档增强完成");
        
        // 3.文档切割
        DocumentByParagraphSplitter documentByParagraphSplitter = new DocumentByParagraphSplitter(100, 20);
        
        // 4.自定义文档加载器，把文档转换成向量并保存到向量数据库中
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentByParagraphSplitter)
                // 提高文档质量
                .textSegmentTransformer(textSegment -> TextSegment.from(textSegment.metadata().getString("file_name") + "\n"
                        + textSegment.text(), textSegment.metadata()))
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        
        // rag加载文档
        log.info("开始向量化文档...");
        ingestor.ingest(enhancedDocs);
        log.info("向量化完成，已保存到Milvus");
    }
}
