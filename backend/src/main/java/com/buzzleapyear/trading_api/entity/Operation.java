package com.buzzleapyear.trading_api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "operations", uniqueConstraints = {
    @UniqueConstraint(columnNames = "operation_name")
})
public class Operation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "operation_id")
    private Long id;
    
    @Column(name = "operation_name", nullable = false, unique = true)
    private String operationName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getOperationName() { return operationName; }
    public void setOperationName(String operationName) { this.operationName = operationName; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
