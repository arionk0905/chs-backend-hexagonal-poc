package com.chs.channel.request.application;

import com.chs.channel.request.application.port.in.ChannelRequests;
import com.chs.channel.request.application.port.out.ChannelRequestStore;
import com.chs.channel.request.domain.ChannelRequest;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

public final class ChannelRequestService implements ChannelRequests {
    private final ChannelRequestStore store;
    private final Clock clock;

    public ChannelRequestService(ChannelRequestStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Override
    public ChannelRequest receive(String subject) {
        var request = new ChannelRequest(
                UUID.randomUUID(), subject, ChannelRequest.Status.RECEIVED, clock.instant());
        store.insert(request);
        return request;
    }

    @Override
    public Optional<ChannelRequest> find(UUID id) {
        return store.find(id);
    }
}
