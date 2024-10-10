package com.alexbalmus.acbblog.modules.blog.dom.post;

import com.alexbalmus.acbblog.modules.blog.dom.blog.Blog;

import lombok.RequiredArgsConstructor;

import jakarta.inject.Inject;

import java.util.List;

import org.apache.causeway.applib.annotation.Collection;
import org.apache.causeway.applib.annotation.CollectionLayout;
import org.apache.causeway.applib.annotation.TableDecorator;

@Collection
@CollectionLayout(defaultView = "table", tableDecorator = TableDecorator.DatatablesNet.class)
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