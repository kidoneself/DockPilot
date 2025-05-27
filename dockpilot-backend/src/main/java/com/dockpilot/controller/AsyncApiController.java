package com.dockpilot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AsyncApiController {

    @Autowired
    private String asyncApiSpec;

    @GetMapping(value = "/asyncapi", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAsyncApiSpec() {
        return asyncApiSpec;
    }
} 