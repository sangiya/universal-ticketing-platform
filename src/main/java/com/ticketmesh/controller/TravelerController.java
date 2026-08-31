package com.ticketmesh.controller;

import com.ticketmesh.dto.SavedTravelerRequest;
import com.ticketmesh.dto.SavedTravelerResponse;
import com.ticketmesh.service.TravelerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/travelers")
public class TravelerController {

    private final TravelerService travelerService;

    public TravelerController(TravelerService travelerService) {
        this.travelerService = travelerService;
    }

    @PostMapping
    public ResponseEntity<SavedTravelerResponse> create(@RequestBody SavedTravelerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(travelerService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<SavedTravelerResponse>> list() {
        return ResponseEntity.ok(travelerService.listMine());
    }

    @GetMapping("/export")
    public ResponseEntity<List<SavedTravelerResponse>> export() {
        return ResponseEntity.ok(travelerService.export());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        travelerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
