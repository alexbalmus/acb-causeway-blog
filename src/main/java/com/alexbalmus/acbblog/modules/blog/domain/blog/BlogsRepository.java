package com.alexbalmus.acbblog.modules.blog.domain.blog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogsRepository extends JpaRepository<Blog, Long>
{
    Blog findByNameAndHandle(final String name, final String handle);
    List<Blog> findByNameContainingOrderByNameAsc(final String name);
    List<Blog> findAllByHandleOrderByNameAsc(final String handle);

    default List<Blog> findByNameContaining(final String name)
    {
        return findByNameContainingOrderByNameAsc(name);
    }

    default List<Blog> findAllByHandle(final String handle)
    {
        return findAllByHandleOrderByNameAsc(handle);
    }
}
