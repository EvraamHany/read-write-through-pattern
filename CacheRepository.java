package com.avaya.digital.smhc.domain.repository.redis;


import com.demo.domain.repository.redis.DatabaseOperations;
import org.redisson.api.MapOptions;
import org.redisson.api.MapOptions.WriteMode;
import org.redisson.api.map.MapLoader;
import org.redisson.api.map.MapWriter;

public interface CacheRepository<T, K> extends DatabaseOperations<T, K> {

    T saveOrUpdateMessage(T message);

    T findMessage(String key);

    void deleteMessage(T message);

    MapLoader<String, T> getReaderOptions();

    MapWriter<String, T> getWriterOptions();

    default MapOptions<String, T> getWriteCachePatternProperties() {
        return MapOptions.<String, T>defaults().writer(getWriterOptions()).writeMode(WriteMode.WRITE_THROUGH);
    }

    default MapOptions<String, T> getReadCachePatternProperties() {
        return MapOptions.<String, T>defaults().loader(getReaderOptions());
    }

}
