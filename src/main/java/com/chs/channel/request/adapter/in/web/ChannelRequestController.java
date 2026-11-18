package com.chs.channel.request.adapter.in.web;

import com.chs.channel.request.application.port.in.ChannelRequests;
import com.chs.channel.request.domain.ChannelRequest;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(ChannelRequestController.PATH)
public class ChannelRequestController {
    static final String PATH = "/api/v1/channel-requests";

    private final ChannelRequests requests;

    public ChannelRequestController(ChannelRequests requests) {
        this.requests = requests;
    }

    @PostMapping
    ResponseEntity<ChannelResponse> receive(@RequestBody ReceiveRequest body) {
        var request = requests.receive(body.subject());
        return ResponseEntity.created(URI.create(PATH + "/" + request.id())).body(toResponse(request));
    }

    @GetMapping("/{id}")
    ChannelResponse find(@PathVariable("id") UUID id) {
        return requests.find(id)
                .map(ChannelRequestController::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "요청을 찾을 수 없습니다."));
    }

    @ExceptionHandler(ChannelRequest.InvalidSubject.class)
    ProblemDetail handleInvalidSubject(ChannelRequest.InvalidSubject error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    private static ChannelResponse toResponse(ChannelRequest request) {
        return new ChannelResponse(request.id(), request.subject(), request.status().name(), request.createdAt());
    }

    record ReceiveRequest(String subject) {}
    record ChannelResponse(UUID id, String subject, String status, Instant createdAt) {}
}
