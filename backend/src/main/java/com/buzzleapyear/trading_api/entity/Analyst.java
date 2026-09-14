package com.buzzleapyear.trading_api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "analysts")
public class Analyst {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analyst_id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Getters and Setters
    public Long getAnalystId() { return id; }
    public void setAnalystId(Long analystId) { this.id = analystId; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
