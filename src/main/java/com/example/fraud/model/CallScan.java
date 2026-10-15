package com.example.fraud.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "call_scans")
public class CallScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender")
    private String sender;

    @Column(name = "channel")
    private String channel;

    @Lob
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "scam_type")
    private String scamType;

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

    @Transient
    private String senderType;

    public CallScan() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getScamType() { return scamType; }
    public void setScamType(String scamType) { this.scamType = scamType; }

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

    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }
}