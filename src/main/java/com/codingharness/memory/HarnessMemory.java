package com.codingharness.memory;

public class HarnessMemory {
    private final MemoryStore store;
    public HarnessMemory(MemoryStore store) { this.store = store; }
    public MemoryStore getStore() { return store; }
}
