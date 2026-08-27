package com.example.demo.generate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.demo.security.AccountPrincipal;
import com.example.demo.works.WorkQueryService;
import org.springframework.dao.DataAccessException;
import org.slf4j.LoggerFactory;

@Controller
public record GenerateController(WorkQueryService works) {
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(defaultValue = "0") int page, Model model) {
        try {
            model.addAttribute("works", works.list(principal.getUserId(), page));
            model.addAttribute("worksError", false);
        } catch (DataAccessException failure) {
            LoggerFactory.getLogger(GenerateController.class).warn("Could not load My Works", failure);
            model.addAttribute("worksError", true);
        }
        return "dashboard";
    }

    @GetMapping("/dashboard/generate")
    public String generate(){
        return "generate";
    }

    @GetMapping("/generate")
    public String legacyGenerate() {
        return "redirect:/dashboard/generate";
    }
}
