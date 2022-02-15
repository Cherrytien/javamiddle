package com.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWord {

    @GetMapping("hello")
    public String hello(){
        return "helloword";
    }
    @GetMapping("blue")
    public String blue(){
        return "helloblue";
    }
}
