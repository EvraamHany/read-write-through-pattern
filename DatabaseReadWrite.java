package com.demo.domain.repository.redis;

@FunctionalInterface
public interface DatabaseReadWrite<T, K> {

    T apply(T t, K k);

}
