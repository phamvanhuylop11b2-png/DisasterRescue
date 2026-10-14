package com.disasterrescue.api.repository;

import com.disasterrescue.api.model.RoleCode;
import com.disasterrescue.api.model.User;
import com.disasterrescue.api.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.username = :login OR u.email = :login")
    Optional<User> findByUsernameOrEmail(@Param("login") String login);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
           "(:q IS NULL OR :q = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "  OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "  OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "AND (:roleCode IS NULL OR u.role.code = :roleCode) " +
           "AND (:status IS NULL OR u.status = :status)")
    Page<User> searchUsers(@Param("q") String q,
                           @Param("roleCode") RoleCode roleCode,
                           @Param("status") UserStatus status,
                           Pageable pageable);
}
