package com.example.fraud.controller;

import com.example.fraud.model.EmailScan;
import com.example.fraud.service.PhishingDetectionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final PhishingDetectionService service;

    public EmailController(PhishingDetectionService service) {
        this.service = service;
    }

    @PostMapping("/scan")
    public EmailScan scan(@RequestBody EmailScan email) {
        return service.scan(email);
    }

    @GetMapping
    public List<EmailScan> getAll() {
        return service.getAll();
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return service.getStats();
    }

    @DeleteMapping
    public String deleteAll() {
        service.deleteAll();
        return "All email scans deleted";
    }
}