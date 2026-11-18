package com.chs.channel.product.adapter.out.core.http;

import com.chs.channel.product.application.port.out.CoreLookupFailure;
import com.chs.channel.product.application.port.out.CoreProductLookup;
import com.chs.channel.product.domain.Product;
import com.chs.channel.product.domain.ProductCode;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class CoreProductHttpAdapter implements CoreProductLookup {
    private static final String PRODUCT_INQUIRY_PATH = "/core/v1/product-inquiries";
    private static final String MESSAGE_ID = "CHS-CORE-001";
    private static final String VERSION = "1.0";
    private static final String SUCCESS = "0000";
    private static final String PRODUCT_NOT_FOUND = "1001";

    private final RestClient client;

    public CoreProductHttpAdapter(RestClient client) {
        this.client = client;
    }

    @Override
    public Optional<Product> find(ProductCode code) {
        var header = new Header(MESSAGE_ID, VERSION, UUID.randomUUID());
        var request = new CoreRequest(header, new RequestBody(code.value()));
        CoreResponse response = send(request);

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

    private CoreResponse send(CoreRequest request) {
        try {
            return client.post()
                    .uri(PRODUCT_INQUIRY_PATH)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.value() != 200, (ignored, response) -> {
                        throw new CoreLookupFailure(CoreLookupFailure.Kind.INVALID_RESPONSE, null);
                    })
                    .body(CoreResponse.class);
        } catch (ResourceAccessException error) {
            Throwable cause = error.getMostSpecificCause();
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                throw new CoreLookupFailure(CoreLookupFailure.Kind.TIMEOUT, error);
            }
            throw new CoreLookupFailure(CoreLookupFailure.Kind.UNAVAILABLE, error);
        } catch (RestClientException error) {
            throw new CoreLookupFailure(CoreLookupFailure.Kind.INVALID_RESPONSE, error);
        }
    }

    record Header(String messageId, String version, UUID requestId) {}
    record RequestBody(String productCode) {}
    record CoreRequest(Header header, RequestBody body) {}
    record ResponseBody(String productCode, String productName) {}
    record CoreResponse(Header header, String resultCode, ResponseBody body) {}
}
