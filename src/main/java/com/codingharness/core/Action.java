package com.codingharness.core;

import java.util.Map;

public record Action(String type, Map<String, Object> parameters) {}
