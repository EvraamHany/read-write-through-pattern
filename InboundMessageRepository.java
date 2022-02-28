package com.demo.domain.repository.redis;

import com.demo.domain.db.model.message;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.api.map.MapLoader;
import org.redisson.api.map.MapWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;


@Slf4j
@Repository
public class InboundMessageRepository implements CacheRepository<message, String> {


    @Autowired(required = false)
    private RedissonClient redisClient;

    private final MessageStateStoreRepository stateStoreRepository;

    @Value("${redis.expireTimeInSeconds}")
    long expireTime;


    public InboundMessageRepository(MessageStateStoreRepository stateStoreRepository) {
        this.stateStoreRepository = stateStoreRepository;
    }

    @Override
    public message saveOrUpdateMessage(message message) {

        if (redisClient == null) {
            return null;
        }

        String messageKey = String.format("%s_%s", message.getStateStoreId().getDigitalAccountId(), message.getStateStoreId().getMessageId());
        RMap<String, message> set = redisClient.getMapCache(messageKey, getWriteCachePatternProperties());
        set.put(messageKey, message);
        set.expire(expireTime, TimeUnit.SECONDS);
        return message;
    }

    @Override
    public message findMessage(String key) {

        if (redisClient == null) {
            return null;
        }
        RMap<String,message> inboundMessages = redisClient.getMapCache(key, getReadCachePatternProperties());
        return inboundMessages != null ? inboundMessages.get(key) : null;
    }

    @Override
    public void deleteMessage(message message) {

        if (redisClient == null) {
            return ;
        }
        String messageKey = String.format("%s_%s", message.getStateStoreId().getDigitalAccountId(), message.getStateStoreId().getMessageId());
        RMap<String, message> map = redisClient.getMapCache(messageKey, getWriteCachePatternProperties());
        map.remove(messageKey);
    }

    @Override
    public MapLoader<message> getReaderOptions() {
        return new MapLoader<>() {
            @Override
            public message load(String key) {
                return loadFromDatabase().apply(null, key);
            }

            @Override
            public Iterable<String> loadAllKeys() {
                return null;
            }
        };
    }

    @Override
    public MapWriter<message> getWriterOptions() {
        return new MapWriter<>() {
            @Override
            public void write(Map<message> map) {
                map.values().forEach(message -> saveInDatabase().apply(message, null));
            }

            @Override
            public void delete(Collection<String> keys) {
                keys.forEach(s -> deleteFromDatabase().apply(null, s));

            }
        };
    }

    @Override
    public DatabaseReadWrite<message, String> saveInDatabase() {
        return (message, key) -> stateStoreRepository.save(message);
    }

    @Override
    public DatabaseReadWrite<message, String> deleteFromDatabase() {
        return (asyncGWStateStoreMessage, key) -> {
            List<String> split = List.of(key.split("_", 2));
            stateStoreRepository.deleteById(message.builder().digitalAccountId(split.get(0)).messageId(split.get(1)).build());
            return asyncGWStateStoreMessage;
        };
    }

    @Override
    public DatabaseReadWrite<message, String> loadFromDatabase() {
        return (asyncGWStateStoreMessage, key) -> {
            List<String> split = List.of(key.split("_", 2));
            return stateStoreRepository.findById(StateStoreMessageId.builder().digitalAccountId(split.get(0)).messageId(split.get(1)).build()).orElse(null);
        };
    }

}
