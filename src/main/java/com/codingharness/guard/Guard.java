package com.codingharness.guard;

import com.codingharness.core.Action;
import com.codingharness.core.ProjectContext;

public interface Guard {
    GuardResult check(Action action, ProjectContext ctx);
}
