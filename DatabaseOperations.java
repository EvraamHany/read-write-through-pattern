package com.demo.domain.repository.redis;

interface DatabaseOperations<T, K> {

    DatabaseReadWrite<T, K> saveInDatabase();

    DatabaseReadWrite<T, K> deleteFromDatabase();

    DatabaseReadWrite<T, K> loadFromDatabase();

}
