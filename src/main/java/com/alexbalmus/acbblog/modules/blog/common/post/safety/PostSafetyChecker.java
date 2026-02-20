package com.alexbalmus.acbblog.modules.blog.common.post.safety;

public interface PostSafetyChecker
{
    SafetyAssessment assess(String title, String content);
}
