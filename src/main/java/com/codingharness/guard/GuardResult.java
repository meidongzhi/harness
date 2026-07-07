package com.codingharness.guard;

public record GuardResult(boolean allowed, String reason, String requiredApproval) {
    public static GuardResult allow() { return new GuardResult(true, "", "SAFE"); }
    public static GuardResult block(String reason) { return new GuardResult(false, reason, "CRITICAL"); }
    public static GuardResult requireApproval(String reason, String level) { return new GuardResult(false, reason, level); }
}
