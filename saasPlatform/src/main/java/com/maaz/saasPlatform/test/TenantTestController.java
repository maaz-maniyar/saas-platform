package com.maaz.saasPlatform.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TenantTestController {

    @GetMapping("/test")
    public String test() {
        return "Tenant OK";
    }
}
