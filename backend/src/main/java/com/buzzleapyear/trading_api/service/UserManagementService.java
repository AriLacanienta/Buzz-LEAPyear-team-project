package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.repository.AnalystRepository;
import com.buzzleapyear.trading_api.repository.ClientRepository;
import com.buzzleapyear.trading_api.repository.ComplianceRepository;
import com.buzzleapyear.trading_api.repository.UserRepository;

public class UserManagementService {

    private UserRepository userRepository;

    public void assignRole(int userId, String role) {
        userRepository.getReferenceById(null)
    }

}
