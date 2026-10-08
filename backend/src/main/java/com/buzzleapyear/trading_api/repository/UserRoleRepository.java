package com.buzzleapyear.trading_api.repository;

import com.buzzleapyear.trading_api.entity.Role;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    
    /**
     * Find all roles assigned to a specific user
     */
    @Query("SELECT ur.role FROM UserRole ur WHERE ur.user.id = :userId")
    List<Role> findRolesByUserId(@Param("userId") Long userId);
    
    /**
     * Find a specific user-role assignment
     */
    Optional<UserRole> findByUserIdAndRoleId(Long userId, Long roleId);
    
    /**
     * Check if a user has a specific role
     */
    @Query("SELECT CASE WHEN COUNT(ur) > 0 THEN true ELSE false END FROM UserRole ur WHERE ur.user.id = :userId AND ur.role.roleName = :roleName")
    boolean userHasRole(@Param("userId") Long userId, @Param("roleName") Role.RoleName roleName);
    
    /**
     * Find all users with a specific role
     */
    @Query("SELECT DISTINCT ur.user FROM UserRole ur WHERE ur.role.roleName = :roleName")
    List<User> findUsersByRoleName(@Param("roleName") Role.RoleName roleName);
    
    /**
     * Find all user-role assignments for a specific user
     */
    List<UserRole> findByUserId(Long userId);
    
    /**
     * Delete a specific user-role assignment
     */
    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}
