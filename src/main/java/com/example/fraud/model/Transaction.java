package com.example.fraud.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "amount")
    private double amount;

    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "location")
    private String location;

    @Column(name = "transaction_time")
    private LocalDateTime transactionTime;

    @Column(name = "fraud_score")
    private int fraudScore;

    @Column(name = "is_fraud")
    private String isFraud;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "actual_label")
    private String actualLabel;

    @Transient
    private List<String> reasons = new ArrayList<>();

    public Transaction() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getTransactionTime() { return transactionTime; }
    public void setTransactionTime(LocalDateTime transactionTime) { this.transactionTime = transactionTime; }

    public int getFraudScore() { return fraudScore; }
    public void setFraudScore(int fraudScore) { this.fraudScore = fraudScore; }

    public String getIsFraud() { return isFraud; }
    public void setIsFraud(String isFraud) { this.isFraud = isFraud; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getActualLabel() { return actualLabel; }
    public void setActualLabel(String actualLabel) { this.actualLabel = actualLabel; }

    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }
}