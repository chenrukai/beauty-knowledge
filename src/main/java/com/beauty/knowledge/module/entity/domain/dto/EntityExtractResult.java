package com.beauty.knowledge.module.entity.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EntityExtractResult {

    private List<String> ingredient = new ArrayList<>();
    private List<String> effect = new ArrayList<>();
    private List<String> product = new ArrayList<>();
}
