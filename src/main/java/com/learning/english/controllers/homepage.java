package com.learning.english.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class homepage {
    @GetMapping("/")
    public String hello(){
        return "homepage";
    }
}
