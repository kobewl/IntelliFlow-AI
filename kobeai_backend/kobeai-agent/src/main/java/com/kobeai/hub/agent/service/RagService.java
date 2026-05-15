package com.kobeai.hub.agent.service;

import io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextReader;
import io.agentscope.core.rag.store.InMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RagService {

    @Value("${dashscope.api.key:}")
    private String dashscopeApiKey;

    private final Map<String, Knowledge> knowledgeBases = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (dashscopeApiKey != null && !dashscopeApiKey.isEmpty()) {
            Knowledge defaultKb = SimpleKnowledge.builder()
                    .embeddingModel(DashScopeTextEmbedding.builder()
                            .apiKey(dashscopeApiKey)
                            .modelName("text-embedding-v3")
                            .dimensions(1024)
                            .build())
                    .embeddingStore(InMemoryStore.builder()
                            .dimensions(1024)
                            .build())
                    .build();
            knowledgeBases.put("default", defaultKb);
            log.info("RAG: 默认知识库已初始化 (embedding=text-embedding-v3, store=InMemory)");
        } else {
            log.warn("RAG: DashScope API Key 未配置, 知识库功能暂不可用");
        }
    }

    public void addDocument(String kbId, String content) {
        Knowledge kb = knowledgeBases.get(kbId);
        if (kb instanceof SimpleKnowledge simpleKb) {
            TextReader reader = new TextReader(512, SplitStrategy.PARAGRAPH, 50);
            List<Document> docs = reader.read(ReaderInput.fromString(content)).block();
            if (docs != null) {
                simpleKb.addDocuments(docs).block();
                log.info("RAG: 已向知识库 [{}] 添加 {} 个文档块", kbId, docs.size());
            }
        }
    }

    public List<String> retrieve(String query, String kbId) {
        Knowledge kb = knowledgeBases.get(kbId);
        if (kb == null) {
            log.warn("RAG: 知识库 [{}] 不存在", kbId);
            return List.of();
        }
        List<Document> results = kb.retrieve(query,
                RetrieveConfig.builder()
                        .limit(3)
                        .scoreThreshold(0.5)
                        .build()
        ).block();

        if (results == null || results.isEmpty()) {
            return List.of();
        }
        return results.stream()
                .map(doc -> doc.getMetadata().getContentText())
                .toList();
    }

    public boolean hasKnowledgeBase(String kbId) {
        return knowledgeBases.containsKey(kbId);
    }
}
