package com.shophub.consultant.repository;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
public class RedisChatMemoryStore implements ChatMemoryStore {
    //娉ㄥ叆RedisTemplate
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        //鑾峰彇浼氳瘽娑堟伅
        String json = redisTemplate.opsForValue().get(memoryId);
        //鎶妀son瀛楃涓茶浆鍖栨垚List<ChatMessage>
        List<ChatMessage> list = ChatMessageDeserializer.messagesFromJson(json);
        return list;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        //鏇存柊浼氳瘽娑堟伅
        //1.鎶妉ist杞崲鎴恓son鏁版嵁
        String json = ChatMessageSerializer.messagesToJson(list);
        //2.鎶妀son鏁版嵁瀛樺偍鍒皉edis涓?
        redisTemplate.opsForValue().set(memoryId.toString(),json, Duration.ofDays(1));
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(memoryId.toString());
    }
}

