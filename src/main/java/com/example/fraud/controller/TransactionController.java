package com.example.fraud.controller;

import com.example.fraud.model.Transaction;
import com.example.fraud.service.FraudDetectionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final FraudDetectionService fraudService;

    public TransactionController(FraudDetectionService fraudService) {
        this.fraudService = fraudService;
    }

    @PostMapping("/analyze")
    public Transaction analyze(@RequestBody Transaction tx) {
        return fraudService.analyze(tx);
    }

    @PostMapping("/analyze-batch")
    public List<Transaction> analyzeBatch(@RequestBody List<Transaction> list) {
        return fraudService.analyzeBatch(list);
    }

    @PutMapping("/{id}/label")
    public Transaction setLabel(@PathVariable Long id, @RequestParam String value) {
        return fraudService.setLabel(id, value);
    }

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        return fraudService.getMetrics();
    }

    @GetMapping
    public List<Transaction> getAll() {
        return fraudService.getAllTransactions();
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return fraudService.getStats();
    }

    @GetMapping("/profile/{userId}")
    public Map<String, Object> getProfile(@PathVariable String userId) {
        return fraudService.getUserProfile(userId);
    }

    @DeleteMapping
    public String deleteAll() {
        fraudService.deleteAll();
        return "All transactions deleted";
    }

    @GetMapping("/test")
    public String test() {
        return "Backend is running!";
    }
}