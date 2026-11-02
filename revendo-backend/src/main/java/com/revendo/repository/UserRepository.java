package com.revendo.repository;

import com.revendo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their unique email address.
     *
     * @param email the email to search for
     * @return an Optional containing the user, or empty if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * Paginated search for admin user management.
     * Matches partial, case-insensitive name or email (Requirement 13.9).
     *
     * When query is null, all users are returned (no filter applied).
     * The COALESCE trick avoids unreliable IS NULL checks on bound String parameters
     * in Hibernate: when :query is null, COALESCE evaluates to '', making both sides
     * of the OR branch return true for any user.
     */
    @org.springframework.data.jpa.repository.Query("""
           SELECT u FROM User u
           WHERE COALESCE(:query, '') = ''
              OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :query, '%'))
           """)
    org.springframework.data.domain.Page<User> findBySearchQuery(
            @org.springframework.data.repository.query.Param("query") String query,
            org.springframework.data.domain.Pageable pageable
    );
}
