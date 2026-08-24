package com.example.demo.generate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public record GenerateController() {
    @GetMapping("/generate")
    public String generate(){
        return "generate";
    }
}
