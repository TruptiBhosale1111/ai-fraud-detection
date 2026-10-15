**🔐 AI-Powered Fraud Detection System**

An intelligent, full-stack fraud detection platform built using Java 21, Spring Boot 3, and MySQL 8.0. 
The system identifies potential fraud across financial transactions, emails, QR codes, phone calls, SMS messages, and job or loan offers using a weighted rule-based scoring engine with explainable risk assessments.

**📌 Project Overview**
Traditional fraud detection systems often focus on suspicious transactions after a scam has already occurred. 
However, fraud can begin much earlier through phishing emails, malicious QR codes, fraudulent phone calls, fake job offers, and predatory loan schemes.

The AI-Powered Fraud Detection System aims to identify these threats before they lead to financial loss by analysing suspicious patterns across five integrated modules.

The platform combines configurable detection rules, user-specific behavioural profiling, risk classification, and analyst feedback to generate transparent and actionable fraud assessments.

**🧩 Core Modules**

1. **💳 Transaction Fraud Detection**
   Identifies unusual transaction amounts and suspicious payment patterns.
   Detects high-frequency transactions and potential velocity attacks.
   Analyses deviations from a user's normal spending behaviour.

2. **📧 Phishing Email Scanner**
   Detects suspicious email content and potentially malicious links.
   Identifies credential-harvesting attempts and OTP theft patterns.
   Flags lottery scams, impersonation attempts, and fraudulent messages.

3. **📱 Fake QR Code Scanner**
   Analyses QR code payloads and embedded payment information.
   Flags suspicious UPI payment links and potentially malicious destinations.
   Identifies patterns associated with fraudulent collect-request schemes.

4. **📞 Call & SMS Fraud Detector**
   Detects messages associated with fake KYC updates and account-blocking threats.
   Flags digital arrest scams, vishing, and smishing patterns.
   Identifies suspicious requests for OTPs, banking credentials, or urgent payments.

5. **💼 Job & Loan Scam Checker**
   Detects advance-fee job offers and suspicious recruitment requests.
   Flags potentially fraudulent loan offers and questionable app-related claims.
   Identifies warning signs such as upfront processing fees and unrealistic promises.

**Risk Levels**

- Score 0 to 29 - SAFE
- Score 30 to 49 - LOW RISK
- Score 50 to 74 - SUSPICIOUS
- Score 75 to 100 - HIGH RISK

**✨ Key Features**
   Weighted Rule-Based Scoring: Assigns configurable weights to detection rules to calculate an overall risk score.
   
   Behavioural Profiling: Builds per-user spending baselines to identify unusual transaction activity.
   
   Explainable Fraud Analysis: Shows the rules triggered, their individual contributions, and the reasoning behind each assessment.
   
   Four-Level Risk Classification: Categorises detected activity into SAFE, LOW RISK, SUSPICIOUS, and HIGH RISK.
    
   Analyst Review Workflow: Supports manual review of predictions against actual outcomes and calculates classification metrics.
   
   Performance Evaluation: Generates a confusion matrix and reports accuracy, precision, recall, and F1-score.
   
   CSV Batch Processing: Analyses multiple records in bulk and supports report export.
   
   Interactive Dashboard: Visualises fraud patterns, risk distributions, and detection statistics using Chart.js.
   
   Configurable Detection Rules: Allows rule weights and thresholds to be adjusted as fraud patterns evolve.

**🛠️ Technology Stack**

Backend :	Java 21, Spring Boot 3
Data Access :	Spring Data JPA, Hibernate
Database :	MySQL 8.0
Frontend :	HTML5, CSS3, Vanilla JavaScript
Data Visualisation :	Chart.js
Build Tool :	Apache Maven
Testing : JUnit 5, Mockito
Code Coverage : JaCoCo

**🏗️ System Architecture**

The application follows a layered architecture to separate user interaction, fraud analysis, business logic, and database operations.

Presentation Layer: Provides the dashboard, input forms, analysis results, and visual reports.

Controller Layer: Receives client requests and exposes backend endpoints.

Service Layer: Executes fraud detection rules, calculates risk scores, and generates explanations.

Data Access Layer: Uses Spring Data JPA and Hibernate to interact with the database.

Database Layer: Stores transaction records, analysis results, behavioural profiles, detection rules, and review data, as implemented.

**Fraud Detection Workflow:**

Input Data → Validation → Rule Evaluation → Risk Scoring → Risk Classification → Explanation → Analyst Review

**Future Enhancements**

- Replace the rule engine with a trained machine learning classifier
- Add user authentication using Spring Security
- Move rule weights into a database with an admin tuning panel
- Add real-time email alerts for high-risk detections
- Automate system testing using Selenium
- Add multi-language support

**Disclaimer**

This is an academic project developed for educational purposes. 
It should not be used for real financial decisions. 
All sample scam data is based on publicly reported fraud patterns and no real user data is collected or stored.

**Author**

[Trupti Bhosale]

