package com.ticketmesh.controller;

import com.ticketmesh.service.MfaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mfa")
public class MfaController {

    private final MfaService mfaService;

    public MfaController(MfaService mfaService) {
        this.mfaService = mfaService;
    }

    @PostMapping("/setup")
    public ResponseEntity<MfaService.SetupResponse> setup() {
        return ResponseEntity.ok(mfaService.setup());
    }

    @PostMapping("/verify")
    public ResponseEntity<MfaService.VerifyResponse> verify(@RequestBody Map<String,String> body) {
        return ResponseEntity.ok(mfaService.verifySetup(body.get("code")));
    }

    @GetMapping("/status")
    public ResponseEntity<MfaService.StatusResponse> status() {
        return ResponseEntity.ok(mfaService.status());
    }

    @PostMapping("/disable")
    public ResponseEntity<Void> disable(@RequestBody Map<String,String> body) {
        mfaService.disable(body.get("code"));
        return ResponseEntity.ok().build();
    }
}
