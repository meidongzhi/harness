package com.codingharness.credentials;

import java.util.Optional;

public interface CredentialStore {
    void store(String key, String value);
    Optional<String> retrieve(String key);
    boolean exists(String key);
    void delete(String key);
    String maskedDisplay(String key);
}
