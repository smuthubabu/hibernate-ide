package com.hibernateide.controller;

import com.hibernateide.model.*;
import com.hibernateide.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    @Autowired
    private QueryService queryService;

    @PostMapping("/explain")
    public ResponseEntity<ApiResponse<String>> explain(@RequestBody QueryRequest request) {
        try {
            String sql = queryService.explainHql(request);
            return ResponseEntity.ok(ApiResponse.ok(sql, "HQL translated to SQL"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<QueryResult>> execute(@RequestBody QueryRequest request) {
        try {
            QueryResult result = queryService.execute(request);
            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.ok(result));
            } else {
                return ResponseEntity.ok(ApiResponse.error(result.getMessage()));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
