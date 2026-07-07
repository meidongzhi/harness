package com.codingharness.guard;

import com.codingharness.core.Action;
import com.codingharness.core.ProjectContext;
import java.util.List;

public class GuardChain {
    private final List<Guard> guards;

    public GuardChain(List<Guard> guards) { this.guards = List.copyOf(guards); }

    public GuardResult check(Action action, ProjectContext ctx) {
        for (Guard guard : guards) {
            GuardResult result = guard.check(action, ctx);
            if (!result.allowed()) return result;
        }
        return GuardResult.allow();
    }
}
