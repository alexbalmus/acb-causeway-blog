package com.alexbalmus.acbblog.modules.blog.domain.homepage.mixins.blog;

import lombok.RequiredArgsConstructor;

import jakarta.inject.Inject;

import java.util.List;

import org.apache.causeway.applib.annotation.Collection;
import org.apache.causeway.applib.annotation.CollectionLayout;
import org.apache.causeway.applib.annotation.TableDecorator;

import com.alexbalmus.acbblog.modules.blog.domain.blog.Blog;
import com.alexbalmus.acbblog.modules.blog.domain.post.Post;
import com.alexbalmus.acbblog.modules.blog.domain.post.PostsRepository;


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