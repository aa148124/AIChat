package com.example.aidemo.ai.config.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RagConfig {

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore) {
        // 1.加载文档
        List<Document> docs = FileSystemDocumentLoader.loadDocuments("src/main/resources/doc");
        // 2.文档切割
        DocumentByParagraphSplitter documentByParagraphSplitter = new DocumentByParagraphSplitter(100, 20);
        // 3.自定义文档加载器，把文档转换成向量并保存到向量数据库中
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentByParagraphSplitter)
                // 提高文档质量
                .textSegmentTransformer(textSegment -> TextSegment.from(textSegment.metadata().getString("file_name") + "\n"
                        + textSegment.text(), textSegment.metadata()))
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        //rag加载文档
        ingestor.ingest(docs);
        //构造内容检索器
        EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(5)
                .build();
        return retriever;
    }
}
