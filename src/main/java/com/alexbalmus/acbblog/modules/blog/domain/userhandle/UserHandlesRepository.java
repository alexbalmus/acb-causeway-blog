package com.alexbalmus.acbblog.modules.blog.domain.userhandle;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHandlesRepository extends JpaRepository<UserHandle, Long>
{
    Optional<UserHandle> findByUsername(String username);
    Optional<UserHandle> findByHandle(String handle);
}
