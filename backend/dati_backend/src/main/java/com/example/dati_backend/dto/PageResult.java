package com.example.dati_backend.dto;

import java.util.List;

public record PageResult<T>(
        List<T> records,
        Long total,
        Integer page,
        Integer size
) {
}
