package com.hibernateide.controller;

import com.hibernateide.model.*;
import com.hibernateide.service.ConnectionService;
import com.hibernateide.service.MappingService;
import com.hibernateide.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private MappingService mappingService;

    @Autowired
    private ProfileService profileService;

    @PostMapping
    public ResponseEntity<ApiResponse<ConnectionInfo>> createConnection(@RequestBody ConnectionConfig config) {
        try {
            ConnectionInfo info = connectionService.createConnection(config);
            return ResponseEntity.ok(ApiResponse.ok(info, "Connection established"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<ConnectionInfo>> testConnection(@RequestBody ConnectionConfig config) {
        try {
            ConnectionInfo info = connectionService.testConnection(config);
            return ResponseEntity.ok(ApiResponse.ok(info, "Connection test successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConnectionInfo>>> listConnections() {
        return ResponseEntity.ok(ApiResponse.ok(connectionService.listConnections()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeConnection(@PathVariable String id) {
        try {
            connectionService.removeConnection(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Connection removed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/mappings")
    public ResponseEntity<ApiResponse<Void>> addMapping(@PathVariable String id, @RequestBody MappingRequest req) {
        try {
            connectionService.addMapping(id, req.getHbmXml());
            return ResponseEntity.ok(ApiResponse.ok(null, "Mapping registered, SessionFactory rebuilt"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/mappings/path")
    public ResponseEntity<ApiResponse<List<EntityInfo>>> loadMappingsFromPath(
            @PathVariable String id, @RequestBody MappingPathRequest req) {
        try {
            List<EntityInfo> entities = mappingService.loadFromPath(id, req);
            // Auto-create or update profile so paths survive backend restarts
            ConnectionInfo info = connectionService.getConnectionInfo(id);
            if (info != null) {
                ConnectionProfile profile = profileService.findByName(info.getName()).orElseGet(() -> {
                    ConnectionProfile p = new ConnectionProfile();
                    p.setName(info.getName());
                    p.setJdbcUrl(info.getJdbcUrl());
                    p.setUsername(info.getUsername());
                    p.setDialect(info.getDialect());
                    p.setDriverClass(info.getDriverClass());
                    try { p.setPassword(connectionService.getConfig(id).getPassword()); } catch (Exception ignored) {}
                    return p;
                });
                profile.setHbmPaths(req.getHbmPaths() != null ? req.getHbmPaths() : new ArrayList<>());
                profile.setClassesPaths(req.getClassesPaths() != null ? req.getClassesPaths() : new ArrayList<>());
                profileService.save(profile);
            }
            return ResponseEntity.ok(ApiResponse.ok(entities,
                    "Loaded " + entities.size() + " entity mapping(s)"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
