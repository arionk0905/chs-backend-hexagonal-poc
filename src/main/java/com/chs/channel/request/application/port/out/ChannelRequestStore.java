package com.chs.channel.request.application.port.out;

import com.chs.channel.request.domain.ChannelRequest;
import java.util.Optional;
import java.util.UUID;

public interface ChannelRequestStore {
    void insert(ChannelRequest request);
    Optional<ChannelRequest> find(UUID id);
}
