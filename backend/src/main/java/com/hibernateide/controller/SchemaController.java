package com.hibernateide.controller;

import com.hibernateide.model.*;
import com.hibernateide.service.MappingService;
import com.hibernateide.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schema")
public class SchemaController {

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private MappingService mappingService;

    @GetMapping("/{connectionId}/tables")
    public ResponseEntity<ApiResponse<List<TableInfo>>> getTables(@PathVariable String connectionId) {
        try {
            List<TableInfo> tables = schemaService.getTables(connectionId);
            return ResponseEntity.ok(ApiResponse.ok(tables));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{connectionId}/tables/{tableName}")
    public ResponseEntity<ApiResponse<TableInfo>> getTableDetail(
            @PathVariable String connectionId,
            @PathVariable String tableName) {
        try {
            TableInfo table = schemaService.getTableDetail(connectionId, tableName);
            return ResponseEntity.ok(ApiResponse.ok(table));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{connectionId}/entities")
    public ResponseEntity<ApiResponse<List<EntityInfo>>> getEntities(@PathVariable String connectionId) {
        return ResponseEntity.ok(ApiResponse.ok(mappingService.getEntities(connectionId)));
    }

    @GetMapping("/{connectionId}/tables/{tableName}/hbm")
    public ResponseEntity<ApiResponse<String>> generateHbm(
            @PathVariable String connectionId,
            @PathVariable String tableName,
            @RequestParam(required = false) String className) {
        try {
            String hbm = schemaService.generateHbmXml(connectionId, tableName, className);
            return ResponseEntity.ok(ApiResponse.ok(hbm));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
