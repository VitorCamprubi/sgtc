package com.vitorcamprubi.sgtc.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/public/health")
    public String health() { return "ok"; }

    @GetMapping("/api/ping")
    public String ping() { return "pong"; } // liberado para health-check com/sem token
}
