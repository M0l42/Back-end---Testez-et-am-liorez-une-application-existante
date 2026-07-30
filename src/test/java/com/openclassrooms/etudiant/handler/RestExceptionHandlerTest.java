package com.openclassrooms.etudiant.handler;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.nio.file.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises each @ExceptionHandler branch directly. The controller integration tests
 * only trigger the 400 and 401 paths, so the 409 / 404 / 403 / 500 handlers are covered here.
 */
class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    private WebRequest webRequest() {
        return new ServletWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse());
    }

    @Test
    void handleConflict_returns_400() {
        ResponseEntity<Object> response =
                handler.handleConflict(new IllegalArgumentException("bad"), webRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ErrorDetails.class);
    }

    @Test
    void handleDataIntegrityViolation_returns_409() {
        ResponseEntity<Object> response = handler.handleDataIntegrityViolationException(
                new DataIntegrityViolationException("duplicate"), webRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleEntityNotFound_returns_404() {
        ResponseEntity<Object> response = handler.handleEntityNotFoundException(
                new EntityNotFoundException("missing"), webRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleBadCredentials_returns_401() {
        ResponseEntity<Object> response = handler.handleBadCredentialsException(
                new BadCredentialsException("nope"), webRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleForbidden_returns_403() {
        ResponseEntity<Object> response = handler.handleForbiddenException(
                new AccessDeniedException("denied"), webRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleException_returns_500() {
        ResponseEntity<Object> response = handler.handleException(
                new RuntimeException("boom"), webRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("Internal Server error");
    }
}
