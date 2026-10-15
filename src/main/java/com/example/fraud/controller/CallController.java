package com.example.fraud.controller;

import com.example.fraud.model.CallScan;
import com.example.fraud.service.CallScamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/calls")
public class CallController {

    private final CallScamService service;

    public CallController(CallScamService service) {
        this.service = service;
    }

    @PostMapping("/scan")
    public CallScan scan(@RequestBody CallScan c) {
        return service.scan(c);
    }

    @GetMapping
    public List<CallScan> getAll() {
        return service.getAll();
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return service.getStats();
    }

    @DeleteMapping
    public String deleteAll() {
        service.deleteAll();
        return "All call scans deleted";
    }
}