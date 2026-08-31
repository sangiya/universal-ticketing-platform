package com.ticketmesh.controller;

import com.ticketmesh.dto.WalletResponse;
import com.ticketmesh.dto.WalletTopUpRequest;
import com.ticketmesh.model.WalletTransaction;
import com.ticketmesh.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<WalletResponse> myWallet() {
        return ResponseEntity.ok(walletService.getMyWallet());
    }

    @PostMapping("/topup")
    public ResponseEntity<WalletResponse> topUp(@RequestBody WalletTopUpRequest req) {
        return ResponseEntity.ok(walletService.topUp(req));
    }

    @GetMapping("/history")
    public ResponseEntity<List<WalletTransaction>> history() {
        return ResponseEntity.ok(walletService.history());
    }
}
