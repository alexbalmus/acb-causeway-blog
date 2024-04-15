package com.alexbalmus.acbblog.modules.blog.dom.blog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogsRepository extends JpaRepository<Blog, Long>
{
    List<Blog> findByNameContaining(final String name);
    List<Blog> findAllByHandle(final String handle);
}
