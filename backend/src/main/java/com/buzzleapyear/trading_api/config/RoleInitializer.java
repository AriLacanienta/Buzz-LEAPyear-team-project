package com.buzzleapyear.trading_api.config;

import com.buzzleapyear.trading_api.entity.Role;
import com.buzzleapyear.trading_api.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes predefined roles in the database on application startup.
 * This ensures that the four required roles (CLIENT, ANALYST, OPERATIONS, COMPLIANCE)
 * are available for assignment to users.
 */
@Component
public class RoleInitializer implements CommandLineRunner {
    
    private final RoleRepository roleRepository;
    
    public RoleInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }
    
    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
    }
    
    private void initializeRoles() {
        // Check if roles already exist
        if (roleRepository.findByRoleName(Role.RoleName.CLIENT).isPresent()) {
            return; // Roles already initialized
        }
        
        // Create and save the four required roles
        roleRepository.save(new Role(
            Role.RoleName.CLIENT,
            "Client role - users who manage their own portfolios and trade"
        ));
        
        roleRepository.save(new Role(
            Role.RoleName.ANALYST,
            "Analyst role - users who analyze market data and create recommendations"
        ));
        
        roleRepository.save(new Role(
            Role.RoleName.OPERATIONS,
            "Operations role - users who manage operational processes and workflows"
        ));
        
        roleRepository.save(new Role(
            Role.RoleName.COMPLIANCE,
            "Compliance role - users who oversee regulatory compliance and audit trails"
        ));
    }
}
