package com.chs.channel.request.application.port.in;

import com.chs.channel.request.domain.ChannelRequest;
import java.util.Optional;
import java.util.UUID;

public interface ChannelRequests {
    ChannelRequest receive(String subject);
    Optional<ChannelRequest> find(UUID id);
}
