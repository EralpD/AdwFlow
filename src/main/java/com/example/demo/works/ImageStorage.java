package com.example.demo.works;

import com.example.demo.agent.visual.VisualGenerationResult;
import org.springframework.core.io.Resource;
import java.util.UUID;

public interface ImageStorage {
    StoredImage store(UUID group, int index, VisualGenerationResult image);
    Resource load(String storageKey);
    void deleteGroup(UUID group);
}
