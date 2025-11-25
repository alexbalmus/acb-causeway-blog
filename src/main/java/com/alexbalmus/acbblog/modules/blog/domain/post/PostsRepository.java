package com.alexbalmus.acbblog.modules.blog.domain.post;

import java.util.List;
import java.util.Optional;

import com.alexbalmus.acbblog.modules.blog.domain.blog.Blog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostsRepository extends JpaRepository<Post, Long>
{
    List<Post> findByBlog(Blog blog);
    Optional<Post> findByBlogAndTitle(Blog blog, String title);
}
