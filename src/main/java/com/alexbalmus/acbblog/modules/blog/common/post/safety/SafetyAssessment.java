package com.alexbalmus.acbblog.modules.blog.common.post.safety;

public record SafetyAssessment(boolean safe, String reason)
{
    public static SafetyAssessment allowed()
    {
        return new SafetyAssessment(true, null);
    }

    public static SafetyAssessment blocked(final String reason)
    {
        return new SafetyAssessment(false, reason);
    }
}
