package com.example.dati_backend.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class CategoryTreeNode {
    private Long id;
    private Long parentId;
    private String name;
    private Integer level;
    private Integer sortNo;
    private String status;
    private List<CategoryTreeNode> children = new ArrayList<>();
}
