package com.project.back_end.repo;

import com.project.back_end.models.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Admin entity.
 * Extends JpaRepository to provide CRUD operations, pagination, and query derivation.
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    /**
     * Custom query method to find an Admin by username.
     * @param username the username of the admin.
     * @return the Admin entity if found, otherwise null.
     */
    Admin findByUsername(String username);
}

