package com.chs.channel.product.adapter.out.core.mci;

import com.chs.channel.product.application.port.out.CoreLookupFailure;
import com.chs.channel.product.application.port.out.CoreProductLookup;
import com.chs.channel.product.domain.Product;
import com.chs.channel.product.domain.ProductCode;
import java.util.Optional;
import java.util.UUID;

public final class MciProductInquiryAdapter implements CoreProductLookup {
    private static final String MESSAGE_ID = "CHS-CORE-001";
    private static final String VERSION = "1.0";
    private static final String SUCCESS = "0000";
    private static final String PRODUCT_NOT_FOUND = "1001";

    private final Transport transport;

    public MciProductInquiryAdapter(Transport transport) {
        this.transport = transport;
    }

    @Override
    public Optional<Product> find(ProductCode code) {
        var header = new Header(MESSAGE_ID, VERSION, UUID.randomUUID());
        var request = new CoreRequest(header, new RequestBody(code.value()));
        CoreResponse response = transport.exchange(request);

        if (response == null || !header.equals(response.header())) {
            throw new CoreLookupFailure(CoreLookupFailure.Kind.INVALID_RESPONSE, null);
        }
        if (PRODUCT_NOT_FOUND.equals(response.resultCode())) {
            return Optional.empty();
        }
        if (!SUCCESS.equals(response.resultCode()) || response.body() == null) {
            throw new CoreLookupFailure(CoreLookupFailure.Kind.INVALID_RESPONSE, null);
        }

        var body = response.body();
        if (!code.value().equals(body.productCode()) || body.productName() == null || body.productName().isBlank()) {
            throw new CoreLookupFailure(CoreLookupFailure.Kind.INVALID_RESPONSE, null);
        }
        return Optional.of(new Product(code, body.productName()));
    }

    @FunctionalInterface
    public interface Transport {
        CoreResponse exchange(CoreRequest request);
    }

    public record Header(String messageId, String version, UUID requestId) {}
    public record RequestBody(String productCode) {}
    public record CoreRequest(Header header, RequestBody body) {}
    public record ResponseBody(String productCode, String productName) {}
    public record CoreResponse(Header header, String resultCode, ResponseBody body) {}
}
