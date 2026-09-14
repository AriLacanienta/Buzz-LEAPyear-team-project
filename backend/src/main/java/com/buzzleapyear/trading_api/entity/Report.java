package com.buzzleapyear.trading_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reports", uniqueConstraints = {
    @UniqueConstraint(columnNames = "report_name")
})
public class Report {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;
    
    @Column(name = "report_name", nullable = false, unique = true)
    private String reportName;
    
    @Column(name = "report_date", nullable = false)
    private LocalDateTime reportDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compliance_id", nullable = false)
    private Compliance compliance;
    
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuditLog> auditLogs;

    // Getters and Setters
    public Long getReportId() { return id; }
    public void setReportId(Long reportId) { this.id = reportId; }
    
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }
    
    public LocalDateTime getReportDate() { return reportDate; }
    public void setReportDate(LocalDateTime reportDate) { this.reportDate = reportDate; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Compliance getCompliance() { return compliance; }
    public void setCompliance(Compliance compliance) { this.compliance = compliance; }
    
    public List<AuditLog> getAuditLogs() { return auditLogs; }
    public void setAuditLogs(List<AuditLog> auditLogs) { this.auditLogs = auditLogs; }
}
