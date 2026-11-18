package com.chs.channel.request.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ChannelRequest(UUID id, String subject, Status status, Instant createdAt) {
    public enum Status { RECEIVED }

    public ChannelRequest {
        Objects.requireNonNull(id);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
        if (subject == null || subject.isBlank() || subject.length() > 120) {
            throw new InvalidSubject();
        }
        subject = subject.strip();
    }

    public static final class InvalidSubject extends IllegalArgumentException {
        public InvalidSubject() {
            super("제목은 공백을 제외한 내용을 포함해야 하며 120자 이하여야 합니다.");
        }
    }
}
