package com.alexbalmus.acbblog.webapp.security;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {}
