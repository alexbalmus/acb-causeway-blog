package com.alexbalmus.acbblog.modules.blog.dom.post;

import com.alexbalmus.acbblog.modules.blog.dom.blog.Blog;

import lombok.RequiredArgsConstructor;

import org.apache.causeway.applib.annotation.Collection;
import org.apache.causeway.applib.annotation.CollectionLayout;

import javax.inject.Inject;

import java.util.List;

@Collection
@CollectionLayout(defaultView = "table")
@RequiredArgsConstructor(onConstructor_ = {@Inject} )
@SuppressWarnings("unused")
public class Blog_posts
{
    private final Blog blog;
    @Inject PostsRepository postsRepository;

    public List<Post> coll()
    {
        return postsRepository.findByBlog(blog);
    }
}