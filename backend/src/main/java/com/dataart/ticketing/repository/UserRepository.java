package com.dataart.ticketing.repository;

import java.util.Optional;
import java.util.UUID;

import com.dataart.ticketing.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
