package com.codingharness.feedback;

import com.codingharness.core.ProjectContext;

public interface FeedbackSensor {
    FeedbackResult sense(ProjectContext ctx);
}
