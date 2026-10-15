package com.example.fraud.controller;

import com.example.fraud.model.QrScan;
import com.example.fraud.service.QrFraudService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/qr")
public class QrController {

    private final QrFraudService service;

    public QrController(QrFraudService service) {
        this.service = service;
    }

    @PostMapping("/scan")
    public QrScan scan(@RequestBody QrScan qr) {
        return service.scan(qr);
    }

    @GetMapping
    public List<QrScan> getAll() {
        return service.getAll();
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return service.getStats();
    }

    @DeleteMapping
    public String deleteAll() {
        service.deleteAll();
        return "All QR scans deleted";
    }
}