package com.study.StudyCafe.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public") // ✅ 공개 API prefix로 변경
@CrossOrigin(origins = "http://52.79.241.126") // 프론트 개발 서버 허용
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        System.out.println("안녕");
        return "Hello Study Cafe!";
    }
}
