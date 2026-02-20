package com.alexbalmus.acbblog.modules.blog.common.post.defaults;

import java.util.Optional;

public interface PostDefaultsGenerator
{
    Optional<PostDefaults> generateForJavaBlogPost();
}
