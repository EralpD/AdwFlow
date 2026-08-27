package com.example.demo.works;

import com.example.demo.workflow.AdvertisingGenerationResult;
import java.util.List;

public record WorkContent(AdvertisingGenerationResult generation, List<StoredImage> images) {
    public WorkContent {
        images = images == null ? List.of() : List.copyOf(images);
    }
}
