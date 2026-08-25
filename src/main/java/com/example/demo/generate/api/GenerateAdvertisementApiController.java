package com.example.demo.generate.api;

import com.example.demo.workflow.AdvertisingGenerationResult;
import com.example.demo.workflow.AdvertisingWorkflow;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/advertisements")
public final class GenerateAdvertisementApiController {

    private final AdvertisingWorkflow workflow;

    public GenerateAdvertisementApiController(
            AdvertisingWorkflow workflow
    ) {
        this.workflow = workflow;
    }

    @PostMapping("/generate")
    public AdvertisingGenerationResult generate(
            @RequestBody GenerateAdvertisementRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Request body must not be null"
            );
        }

        return workflow.generateAdvertisement(
                request.toCommand()
        );
    }
}
