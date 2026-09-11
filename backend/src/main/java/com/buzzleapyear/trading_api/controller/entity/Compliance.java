package com.buzzleapyear.trading_api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "compliance")
public class Compliance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "compliance_id")
    private Long complianceId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "compliance_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ComplianceStatus complianceStatus;
    
    @OneToOne(mappedBy = "compliance")
    private Report report;

    // Getters and Setters
    public Long getComplianceId() { return complianceId; }
    public void setComplianceId(Long complianceId) { this.complianceId = complianceId; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public ComplianceStatus getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(ComplianceStatus complianceStatus) { this.complianceStatus = complianceStatus; }
    
    public Report getReport() { return report; }
    public void setReport(Report report) { this.report = report; }
    
    public enum ComplianceStatus {
        PENDING, APPROVED, REJECTED
    }
}
