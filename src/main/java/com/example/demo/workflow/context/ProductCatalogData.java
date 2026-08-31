package com.example.demo.workflow.context;

import java.util.List;

public record ProductCatalogData(
        String productId,
        String name,
        Integer capacityMl,
        List<String> features,
        ProductClaims claims
) {
    public ProductCatalogData {
        features = features == null ? List.of() : List.copyOf(features);
    }
}
