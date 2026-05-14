package com.example.dati_backend.controller;

import com.example.dati_backend.common.ApiResponse;
import com.example.dati_backend.dto.PageResult;
import com.example.dati_backend.dto.RankingItem;
import com.example.dati_backend.service.RankingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RankingController {
    private final RankingService rankingService;

    @GetMapping("/api/rankings")
    public ApiResponse<List<RankingItem>> ranking(
            @RequestParam(defaultValue = "50") Integer limit,
            @RequestParam(required = false) String sort
    ) {
        return ApiResponse.ok(rankingService.ranking(limit, sort));
    }

    @GetMapping("/api/admin/rankings")
    public ApiResponse<PageResult<RankingItem>> adminRanking(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return ApiResponse.ok(rankingService.pageRanking(page, size));
    }

    @GetMapping("/api/admin/rankings/export")
    public ResponseEntity<byte[]> exportAdminRanking() {
        byte[] content = rankingService.buildRankingExport();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("ranking-export.xlsx")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}
