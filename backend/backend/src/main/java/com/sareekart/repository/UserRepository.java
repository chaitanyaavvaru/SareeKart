package com.sareekart.repository;

import com.sareekart.entity.User;
import com.sareekart.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.email = ?1")
    Optional<User> findByEmailWithRoles(String email);

    long countByRoles_Name(RoleName roleName);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = ?1 ORDER BY u.createdAt DESC")
    List<User> findAllByRole(RoleName roleName);
}