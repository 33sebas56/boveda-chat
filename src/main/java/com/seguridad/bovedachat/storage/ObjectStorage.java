package com.seguridad.bovedachat.storage;
public interface ObjectStorage {
    void put(String key,byte[] encrypted);
    byte[] get(String key);
    void delete(String key);
}
