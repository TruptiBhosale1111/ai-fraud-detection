package com.example.fraud.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "email_scans")
public class EmailScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender")
    private String sender;

    @Column(name = "subject")
    private String subject;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "risk_score")
    private int riskScore;

    @Column(name = "verdict")
    private String verdict;

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    @Transient
    private List<String> reasons = new ArrayList<>();

    @Transient
    private List<String> links = new ArrayList<>();

    @Transient
    private String advice;

    public EmailScan() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public LocalDateTime getScannedAt() { return scannedAt; }
    public void setScannedAt(LocalDateTime scannedAt) { this.scannedAt = scannedAt; }

    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }

    public List<String> getLinks() { return links; }
    public void setLinks(List<String> links) { this.links = links; }

    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }
}