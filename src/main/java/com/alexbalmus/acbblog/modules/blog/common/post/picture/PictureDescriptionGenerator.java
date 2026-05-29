package com.alexbalmus.acbblog.modules.blog.common.post.picture;

import java.util.Optional;

import org.apache.causeway.applib.value.Blob;

public interface PictureDescriptionGenerator
{
    Optional<String> generateFor(Blob picture);
}
