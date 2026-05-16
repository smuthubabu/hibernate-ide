package com.hibernateide.controller;

import com.hibernateide.model.ApiResponse;
import com.hibernateide.model.ConnectionProfile;
import com.hibernateide.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConnectionProfile>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> save(@RequestBody ConnectionProfile profile) {
        if (profile.getName() == null || profile.getName().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Profile name is required"));
        }
        profileService.save(profile);
        return ResponseEntity.ok(ApiResponse.ok(null, "Profile saved"));
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String name) {
        profileService.delete(name);
        return ResponseEntity.ok(ApiResponse.ok(null, "Profile deleted"));
    }
}
