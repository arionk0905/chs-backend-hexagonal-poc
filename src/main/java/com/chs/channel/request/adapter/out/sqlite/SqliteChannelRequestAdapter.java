package com.chs.channel.request.adapter.out.sqlite;

import com.chs.channel.request.application.port.out.ChannelRequestStore;
import com.chs.channel.request.domain.ChannelRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class SqliteChannelRequestAdapter implements ChannelRequestStore {
    private final JdbcClient jdbc;

    public SqliteChannelRequestAdapter(JdbcClient jdbc) {
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
                .param("createdAt", request.createdAt().toString())
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
                        Instant.parse(row.getString("created_at"))))
                .optional();
    }
}
