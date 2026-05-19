package com.gym.fitness.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping(value = {
            "/login",
            "/register",
            "/student/**",
            "/coach/**",
            "/admin/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
