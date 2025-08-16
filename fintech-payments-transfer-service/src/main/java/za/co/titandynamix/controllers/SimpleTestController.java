package za.co.titandynamix.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class SimpleTestController {

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        System.out.println("=== DEBUG: SimpleTestController.ping() called ===");
        return ResponseEntity.ok("pong");
    }
    
    @GetMapping("/debug")
    public ResponseEntity<String> debug() {
        System.out.println("=== DEBUG: SimpleTestController.debug() called ===");
        return ResponseEntity.ok("Debug endpoint working - breakpoints should work here too!");
    }
}