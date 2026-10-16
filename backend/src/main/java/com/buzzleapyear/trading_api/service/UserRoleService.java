package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.entity.Role;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.entity.UserRole;
import com.buzzleapyear.trading_api.repository.RoleRepository;
import com.buzzleapyear.trading_api.repository.UserRepository;
import com.buzzleapyear.trading_api.repository.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user roles and permissions.
 * Provides methods to assign, remove, and check user roles.
 */
@Service
public class UserRoleService {
    
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    
    public UserRoleService(UserRoleRepository userRoleRepository, 
                           RoleRepository roleRepository, 
                           UserRepository userRepository) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Assign a role to a user
     * @param userId the ID of the user
     * @param roleName the role to assign
     * @param assignedBy the username of who assigned the role (for audit trail)
     * @return the created UserRole
     */
    @Transactional
    public UserRole assignRoleToUser(Long userId, Role.RoleName roleName, String assignedBy) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        Role role = roleRepository.findByRoleName(roleName)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        
        // Check if role already assigned
        if (userRoleRepository.findByUserIdAndRoleId(userId, role.getId()).isPresent()) {
            throw new IllegalArgumentException("User already has role: " + roleName);
        }
        
        UserRole userRole = new UserRole(user, role, assignedBy);
        return userRoleRepository.save(userRole);
    }
    
    /**
     * Remove a role from a user
     * @param userId the ID of the user
     * @param roleName the role to remove
     */
    @Transactional
    public void removeRoleFromUser(Long userId, Role.RoleName roleName) {
        Role role = roleRepository.findByRoleName(roleName)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        
        userRoleRepository.deleteByUserIdAndRoleId(userId, role.getId());
    }
    
    /**
     * Get all roles assigned to a user
     * @param userId the ID of the user
     * @return list of role names assigned to the user
     */
    @Transactional(readOnly = true)
    public List<Role.RoleName> getUserRoles(Long userId) {
        return userRoleRepository.findRolesByUserId(userId)
            .stream()
            .map(role -> role.getRoleName())
            .collect(Collectors.toList());
    }
    
    /**
     * Check if a user has a specific role
     * @param userId the ID of the user
     * @param roleName the role to check
     * @return true if the user has the role, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean userHasRole(Long userId, Role.RoleName roleName) {
        return userRoleRepository.userHasRole(userId, roleName);
    }
    
    /**
     * Get all users with a specific role
     * @param roleName the role to filter by
     * @return list of users with the specified role
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(Role.RoleName roleName) {
        return userRoleRepository.findUsersByRoleName(roleName);
    }
    
    /**
     * Replace all roles for a user (useful for the security management page)
     * @param userId the ID of the user
     * @param roleNames the new set of roles to assign
     * @param assignedBy the username of who made the change (for audit trail)
     */
    @Transactional
    public void updateUserRoles(Long userId, List<Role.RoleName> roleNames, String assignedBy) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        // Remove all existing roles
        List<UserRole> existingRoles = userRoleRepository.findByUserId(userId);
        userRoleRepository.deleteAll(existingRoles);
        
        // Assign new roles
        for (Role.RoleName roleName : roleNames) {
            Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
            
            UserRole userRole = new UserRole(user, role, assignedBy);
            userRoleRepository.save(userRole);
        }
    }
    
    /**
     * Get role by role name
     * @param roleName the role name to lookup
     * @return the Role entity
     */
    @Transactional(readOnly = true)
    public Role getRoleByName(Role.RoleName roleName) {
        return roleRepository.findByRoleName(roleName)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
    }
}
