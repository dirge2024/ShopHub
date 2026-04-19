package com.shophub.consultant.config;


import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class  CommonConfig {
    @Autowired
    private OpenAiChatModel model;
    @Autowired
    private ChatMemoryStore redisChatMemoryStore;
    @Autowired
    private EmbeddingModel embeddingModel;
//    @Autowired
//    private RedisEmbeddingStore redisEmbeddingStore;
    /*@Bean
    public ConsultantService consultantService(){
        ConsultantService consultantService = AiServices.builder(ConsultantService.class)
                .chatModel(model)
                .build();
        return consultantService;
    }*/

    //鏋勫缓浼氳瘽璁板繂瀵硅薄
    @Bean
    public ChatMemory chatMemory(){
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
        return memory;
    }

    //鏋勫缓ChatMemoryProvider瀵硅薄
    @Bean
    public ChatMemoryProvider chatMemoryProvider(){
        ChatMemoryProvider chatMemoryProvider = new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) {
                return MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(redisChatMemoryStore)
                        .build();
            }
        };
        return chatMemoryProvider;
    }

    //鏋勫缓鍚戦噺鏁版嵁搴撴搷浣滃璞?
    //@Bean
    public EmbeddingStore store(){//embeddingStore鐨勫璞? 杩欎釜瀵硅薄鐨勫悕瀛椾笉鑳介噸澶?鎵€浠ヨ繖閲屼娇鐢╯tore
        //1.鍔犺浇鏂囨。杩涘唴瀛?
        List<Document> documents = ClassPathDocumentLoader.loadDocuments("content");
        //2.鏋勫缓鍚戦噺鏁版嵁搴撴搷浣滃璞? 鎿嶄綔鐨勬槸鍐呭瓨鐗堟湰鐨勫悜閲忔暟鎹簱
        InMemoryEmbeddingStore store = new InMemoryEmbeddingStore();

        //鏋勫缓鏂囨。鍒嗗壊鍣ㄥ璞?
        DocumentSplitter ds = DocumentSplitters.recursive(500,100);
        //3.鏋勫缓涓€涓狤mbeddingStoreIngestor瀵硅薄,瀹屾垚鏂囨湰鏁版嵁鍒囧壊,鍚戦噺鍖? 瀛樺偍
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(store)
//                .embeddingStore(redisEmbeddingStore)
                .documentSplitter(ds)
                .embeddingModel(embeddingModel)
                .build();
        ingestor.ingest(documents);
        return store;
    }

    //鏋勫缓鍚戦噺鏁版嵁搴撴绱㈠璞?
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore store){
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .minScore(0.5)
                .maxResults(3)
                .embeddingModel(embeddingModel)
                .build();
    }
}
