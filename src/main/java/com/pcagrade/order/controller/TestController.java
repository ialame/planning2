package com.pcagrade.order.controller;

import com.pcagrade.order.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/public/test")
public class TestController {

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter testSSE() {
        SseEmitter emitter = new SseEmitter();

        try {
            emitter.send(SseEmitter.event()
                    .name("test")
                    .data("SSE works!"));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }
}