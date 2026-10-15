package com.example.fraud.service;

import com.example.fraud.model.Transaction;
import com.example.fraud.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class FraudDetectionService {

    private final TransactionRepository repository;

    public FraudDetectionService(TransactionRepository repository) {
        this.repository = repository;
    }

    public Transaction analyze(Transaction tx) {

        if (tx.getTransactionTime() == null) {
            tx.setTransactionTime(LocalDateTime.now());
        }

        if (tx.getUserId() == null || tx.getUserId().trim().isEmpty()) {
            tx.setUserId("GUEST");
        }

        int score = 0;
        List<String> reasons = new ArrayList<>();

        String loc = (tx.getLocation() == null) ? "" : tx.getLocation().toUpperCase();
        String type = (tx.getTransactionType() == null) ? "" : tx.getTransactionType().toUpperCase();

        // RULE 1: High amount
        if (tx.getAmount() > 10000) {
            score += 40;
            reasons.add("Very high amount (Rs " + tx.getAmount() + ") [+40]");
        } else if (tx.getAmount() > 5000) {
            score += 20;
            reasons.add("High amount (Rs " + tx.getAmount() + ") [+20]");
        }

        // RULE 2: Odd hour
        int hour = tx.getTransactionTime().getHour();
        if (hour >= 0 && hour <= 5) {
            score += 25;
            reasons.add("Unusual time (" + hour + ":00 hrs) [+25]");
        }

        // RULE 3: Risky location
        if (loc.contains("UNKNOWN") || loc.contains("HIGH-RISK") || loc.contains("FOREIGN")) {
            score += 25;
            reasons.add("Suspicious location (" + tx.getLocation() + ") [+25]");
        }

        // RULE 4: Velocity
        LocalDateTime tenMinutesAgo = tx.getTransactionTime().minusMinutes(10);
        long recentCount = repository.countByTransactionTimeBetween(tenMinutesAgo, tx.getTransactionTime());
        if (recentCount >= 3) {
            score += 30;
            reasons.add("Too many transactions (" + recentCount + " in 10 min) [+30]");
        }

        // RULE 5: Large withdrawal
        if (type.equals("WITHDRAWAL") && tx.getAmount() > 3000) {
            score += 15;
            reasons.add("Large cash withdrawal [+15]");
        }

        // RULE 6 & 7: User behaviour profiling
        List<Transaction> history = repository.findByUserId(tx.getUserId());

        if (history.size() >= 3) {

            double sum = 0;
            Set<String> knownLocations = new HashSet<>();

            for (Transaction h : history) {
                sum += h.getAmount();
                if (h.getLocation() != null) {
                    knownLocations.add(h.getLocation().toUpperCase());
                }
            }

            double avg = sum / history.size();
            long avgRounded = Math.round(avg);

            if (avg > 0 && tx.getAmount() > avg * 5) {
                score += 30;
                long times = Math.round(tx.getAmount() / avg);
                reasons.add("Amount is " + times + "x the user's average (Rs " + avgRounded + ") [+30]");
            } else if (avg > 0 && tx.getAmount() > avg * 3) {
                score += 15;
                reasons.add("Amount is 3x+ the user's average (Rs " + avgRounded + ") [+15]");
            }

            if (!loc.isEmpty() && !knownLocations.contains(loc)) {
                score += 20;
                reasons.add("New location for this user (never used before) [+20]");
            }
        }

        if (score > 100) {
            score = 100;
        }

        if (reasons.isEmpty()) {
            reasons.add("No suspicious pattern detected");
        }

        String riskLevel;
        if (score < 30) {
            riskLevel = "SAFE";
        } else if (score < 50) {
            riskLevel = "LOW RISK";
        } else if (score < 75) {
            riskLevel = "SUSPICIOUS";
        } else {
            riskLevel = "HIGH RISK";
        }

        String result = (score >= 50) ? "FRAUD" : "LEGIT";

        tx.setFraudScore(score);
        tx.setIsFraud(result);
        tx.setRiskLevel(riskLevel);

        Transaction saved = repository.save(tx);
        saved.setReasons(reasons);

        return saved;
    }

    public List<Transaction> analyzeBatch(List<Transaction> list) {

        List<Transaction> results = new ArrayList<>();

        for (Transaction tx : list) {
            results.add(analyze(tx));
        }

        return results;
    }

    public Transaction setLabel(Long id, String label) {

        Optional<Transaction> found = repository.findById(id);

        if (found.isEmpty()) {
            return null;
        }

        Transaction tx = found.get();

        if ("CLEAR".equalsIgnoreCase(label)) {
            tx.setActualLabel(null);
        } else {
            tx.setActualLabel(label.toUpperCase());
        }

        return repository.save(tx);
    }

    public Map<String, Object> getMetrics() {

        List<Transaction> all = repository.findAll();

        long tp = 0;
        long fp = 0;
        long tn = 0;
        long fn = 0;

        for (Transaction t : all) {

            String actual = t.getActualLabel();

            if (actual == null) {
                continue;
            }

            boolean predictedFraud = "FRAUD".equals(t.getIsFraud());
            boolean actualFraud = "FRAUD".equals(actual);

            if (predictedFraud && actualFraud) {
                tp++;
            } else if (predictedFraud && !actualFraud) {
                fp++;
            } else if (!predictedFraud && !actualFraud) {
                tn++;
            } else {
                fn++;
            }
        }

        long labeled = tp + fp + tn + fn;

        double accuracy = 0;
        double precision = 0;
        double recall = 0;
        double f1 = 0;
        double specificity = 0;

        if (labeled > 0) {
            accuracy = ((tp + tn) * 100.0) / labeled;
        }
        if (tp + fp > 0) {
            precision = (tp * 100.0) / (tp + fp);
        }
        if (tp + fn > 0) {
            recall = (tp * 100.0) / (tp + fn);
        }
        if (tn + fp > 0) {
            specificity = (tn * 100.0) / (tn + fp);
        }
        if (precision + recall > 0) {
            f1 = (2 * precision * recall) / (precision + recall);
        }

        Map<String, Object> m = new HashMap<>();
        m.put("truePositive", tp);
        m.put("falsePositive", fp);
        m.put("trueNegative", tn);
        m.put("falseNegative", fn);
        m.put("labeled", labeled);
        m.put("unlabeled", all.size() - labeled);
        m.put("accuracy", round1(accuracy));
        m.put("precision", round1(precision));
        m.put("recall", round1(recall));
        m.put("f1Score", round1(f1));
        m.put("specificity", round1(specificity));

        return m;
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    public List<Transaction> getAllTransactions() {
        return repository.findAll();
    }

    public Map<String, Object> getUserProfile(String userId) {

        List<Transaction> history = repository.findByUserId(userId);

        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", userId);
        profile.put("txCount", history.size());

        if (history.isEmpty()) {
            profile.put("avgAmount", 0);
            profile.put("maxAmount", 0);
            profile.put("fraudCount", 0);
            profile.put("locations", new ArrayList<String>());
            profile.put("status", "NO DATA");
            return profile;
        }

        double sum = 0;
        double max = 0;
        long fraudCount = 0;
        Set<String> locations = new HashSet<>();

        for (Transaction t : history) {
            sum += t.getAmount();
            if (t.getAmount() > max) {
                max = t.getAmount();
            }
            if ("FRAUD".equals(t.getIsFraud())) {
                fraudCount++;
            }
            if (t.getLocation() != null) {
                locations.add(t.getLocation());
            }
        }

        double avg = sum / history.size();

        String status;
        if (history.size() < 3) {
            status = "LEARNING";
        } else if (fraudCount == 0) {
            status = "TRUSTED";
        } else if (fraudCount > history.size() / 2) {
            status = "HIGH RISK USER";
        } else {
            status = "MONITORED";
        }

        profile.put("avgAmount", Math.round(avg));
        profile.put("maxAmount", max);
        profile.put("fraudCount", fraudCount);
        profile.put("locations", new ArrayList<>(locations));
        profile.put("status", status);

        return profile;
    }

    public Map<String, Object> getStats() {

        List<Transaction> all = repository.findAll();

        long total = all.size();
        long fraud = 0;
        long legit = 0;
        double fraudAmount = 0;
        double totalAmount = 0;

        long safe = 0;
        long lowRisk = 0;
        long suspicious = 0;
        long highRisk = 0;

        Set<String> users = new HashSet<>();

        for (Transaction t : all) {
            totalAmount += t.getAmount();

            if (t.getUserId() != null) {
                users.add(t.getUserId());
            }

            if ("FRAUD".equals(t.getIsFraud())) {
                fraud++;
                fraudAmount += t.getAmount();
            } else {
                legit++;
            }

            String level = t.getRiskLevel();
            if ("SAFE".equals(level)) {
                safe++;
            } else if ("LOW RISK".equals(level)) {
                lowRisk++;
            } else if ("SUSPICIOUS".equals(level)) {
                suspicious++;
            } else if ("HIGH RISK".equals(level)) {
                highRisk++;
            }
        }

        double fraudRate = 0;
        if (total > 0) {
            fraudRate = (fraud * 100.0) / total;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("fraud", fraud);
        stats.put("legit", legit);
        stats.put("fraudRate", round1(fraudRate));
        stats.put("fraudAmount", fraudAmount);
        stats.put("totalAmount", totalAmount);
        stats.put("users", users.size());
        stats.put("safe", safe);
        stats.put("lowRisk", lowRisk);
        stats.put("suspicious", suspicious);
        stats.put("highRisk", highRisk);

        return stats;
    }

    public void deleteAll() {
        repository.deleteAll();
    }
}