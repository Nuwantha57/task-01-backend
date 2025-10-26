package org.eyepax.staffauth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Cognito handles logout via /logout endpoint; API just acknowledges
        return ResponseEntity.ok().build();
    }
}
