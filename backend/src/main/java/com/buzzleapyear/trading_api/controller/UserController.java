package com.buzzleapyear.trading_api.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buzzleapyear.trading_api.repository.UserRepository;

@RestController 
@RequestMapping("/api/v1")
public class UserController {

    private UserRepository users;
    
    @PostMapping("/{userId}")
    public void assignRole(@PathVariable int userID, @RequestParam String role){
        
    }
}
