package com.chs.channel.request.adapter.out.mariadb;

import com.chs.channel.request.application.port.out.ChannelRequestStore;
import com.chs.channel.request.domain.ChannelRequest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class MariaDbChannelRequestAdapter implements ChannelRequestStore {
    private final JdbcClient jdbc;

    public MariaDbChannelRequestAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(ChannelRequest request) {
        jdbc.sql("""
                INSERT INTO channel_request (id, subject, status, created_at)
                VALUES (:id, :subject, :status, :createdAt)
                """)
                .param("id", request.id().toString())
                .param("subject", request.subject())
                .param("status", request.status().name())
                .param("createdAt", LocalDateTime.ofInstant(request.createdAt(), ZoneOffset.UTC))
                .update();
    }

    @Override
    public Optional<ChannelRequest> find(UUID id) {
        return jdbc.sql("""
                SELECT id, subject, status, created_at
                FROM channel_request
                WHERE id = :id
                """)
                .param("id", id.toString())
                .query((row, index) -> new ChannelRequest(
                        UUID.fromString(row.getString("id")),
                        row.getString("subject"),
                        ChannelRequest.Status.valueOf(row.getString("status")),
                        row.getObject("created_at", LocalDateTime.class).toInstant(ZoneOffset.UTC)))
                .optional();
    }
}
