package com.example.fraud.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "offer_scans")
public class OfferScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offer_type")
    private String offerType;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "contact_info")
    private String contactInfo;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "scam_type")
    private String scamType;

    @Column(name = "fee_demanded")
    private String feeDemanded;

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
    private String contactType;

    public OfferScan() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOfferType() { return offerType; }
    public void setOfferType(String offerType) { this.offerType = offerType; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getScamType() { return scamType; }
    public void setScamType(String scamType) { this.scamType = scamType; }

    public String getFeeDemanded() { return feeDemanded; }
    public void setFeeDemanded(String feeDemanded) { this.feeDemanded = feeDemanded; }

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

    public String getContactType() { return contactType; }
    public void setContactType(String contactType) { this.contactType = contactType; }
}