package com.codingharness.credentials;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AesCredentialStore implements CredentialStore {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256;

    private final Path storeFile;
    private final String masterPassword;
    private final Map<String, String> cache;

    public AesCredentialStore(Path directory, String masterPassword) {
        this.storeFile = directory.resolve("credentials.enc");
        this.masterPassword = masterPassword;
        this.cache = new HashMap<>();
        loadFromDisk();
    }

    @Override
    public void store(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        cache.put(key, value);
        persist();
    }

    @Override
    public Optional<String> retrieve(String key) {
        return Optional.ofNullable(cache.get(key));
    }

    @Override
    public boolean exists(String key) {
        return cache.containsKey(key);
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
        persist();
    }

    @Override
    public String maskedDisplay(String key) {
        String value = cache.get(key);
        if (value == null) {
            return "*** (not set)";
        }
        if (value.length() <= 8) {
            return "***-" + value.substring(Math.max(0, value.length() - 4));
        } else {
            return "***-" + value.substring(value.length() - 6);
        }
    }

    private void loadFromDisk() {
        if (!Files.exists(storeFile)) {
            return;
        }
        try {
            byte[] fileContent = Files.readAllBytes(storeFile);
            if (fileContent.length < GCM_IV_LENGTH + 1) {
                return;
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(fileContent, 0, iv, 0, GCM_IV_LENGTH);
            byte[] encryptedData = new byte[fileContent.length - GCM_IV_LENGTH];
            System.arraycopy(fileContent, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);

            SecretKey key = deriveKey(masterPassword);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            byte[] decrypted = cipher.doFinal(encryptedData);

            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decrypted))) {
                @SuppressWarnings("unchecked")
                Map<String, String> loaded = (Map<String, String>) ois.readObject();
                cache.clear();
                cache.putAll(loaded);
            }
        } catch (Exception e) {
            System.err.println("WARNING: Failed to load credential store, starting with empty cache: " + e.getMessage());
        }
    }

    private void persist() {
        try {
            Files.createDirectories(storeFile.getParent());

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(new HashMap<>(cache));
            }
            byte[] plainData = bos.toByteArray();

            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            SecretKey key = deriveKey(masterPassword);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] encryptedData = cipher.doFinal(plainData);

            byte[] output = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, output, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, output, GCM_IV_LENGTH, encryptedData.length);

            Files.write(storeFile, output);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist credential store", e);
        }
    }

    private SecretKey deriveKey(String password) throws Exception {
        byte[] salt = "coding-harness-salt".getBytes();
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
}
