package com.example.demo.works;

import com.example.demo.security.AccountPrincipal;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/dashboard/works")
public class WorkController {
    private final WorkQueryService works;
    private final ImageStorage storage;
    public WorkController(WorkQueryService works, ImageStorage storage) { this.works = works; this.storage = storage; }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal principal, Model model) {
        model.addAttribute("work", works.detail(principal.getUserId(), id));
        return "work";
    }

    @GetMapping("/{id}/images/{index}")
    @ResponseBody
    public ResponseEntity<Resource> image(@PathVariable Long id, @PathVariable int index,
            @RequestParam(defaultValue = "false") boolean download, @AuthenticationPrincipal AccountPrincipal principal) {
        var image = works.image(principal.getUserId(), id, index);
        var disposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename("adwflow-" + id + "-" + (index + 1) + ".png").build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff").body(storage.load(image.storageKey()));
    }
}
