package com.example.dati_backend.dto;

public record CategoryRequest(
        Long parentId,
        String name,
        Integer level,
        Integer sortNo,
        String status
) {
}
