package com.accel.finanzas;

import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    record Ping(String estado, String app, LocalDateTime hora) {}

    @GetMapping("/ping")
    public Ping ping() {
        return new Ping("ok", "Mis Finanzas", LocalDateTime.now());
    }
}
