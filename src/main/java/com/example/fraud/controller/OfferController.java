package com.example.fraud.controller;

import com.example.fraud.model.OfferScan;
import com.example.fraud.service.OfferScamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/offers")
public class OfferController {

    private final OfferScamService service;

    public OfferController(OfferScamService service) {
        this.service = service;
    }

    @PostMapping("/scan")
    public OfferScan scan(@RequestBody OfferScan o) {
        return service.scan(o);
    }

    @GetMapping
    public List<OfferScan> getAll() {
        return service.getAll();
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return service.getStats();
    }

    @DeleteMapping
    public String deleteAll() {
        service.deleteAll();
        return "All offer scans deleted";
    }
}