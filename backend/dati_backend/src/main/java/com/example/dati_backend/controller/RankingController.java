package com.example.dati_backend.controller;

import com.example.dati_backend.common.ApiResponse;
import com.example.dati_backend.dto.RankingItem;
import com.example.dati_backend.service.RankingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
}
