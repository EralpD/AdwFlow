package com.example.demo.generate.api;

import com.example.demo.works.SavedWorkGenerationService;
import com.example.demo.security.AccountPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/advertisements")
public final class GenerateAdvertisementApiController {

    private final SavedWorkGenerationService works;

    public GenerateAdvertisementApiController(
            SavedWorkGenerationService works
    ) {
        this.works = works;
    }

    @PostMapping("/generate")
    public SavedAdvertisementResponse generate(
            @RequestBody GenerateAdvertisementRequest request,
            @AuthenticationPrincipal AccountPrincipal principal
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Request body must not be null"
            );
        }

        return SavedAdvertisementResponse.from(works.generate(principal, request.toCommand()));
    }
}
