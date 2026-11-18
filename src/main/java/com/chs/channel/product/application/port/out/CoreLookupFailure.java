package com.chs.channel.product.application.port.out;

public final class CoreLookupFailure extends RuntimeException {
    public enum Kind { UNAVAILABLE, INVALID_RESPONSE, TIMEOUT }

    private final Kind kind;

    public CoreLookupFailure(Kind kind, Throwable cause) {
        super(kind.name(), cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
