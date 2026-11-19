package com.chs.channel.product.adapter.out.core.mci;

import com.chs.channel.product.adapter.out.core.mci.MciProductInquiryAdapter.CoreRequest;
import com.chs.channel.product.adapter.out.core.mci.MciProductInquiryAdapter.CoreResponse;
import com.chs.channel.product.application.port.out.CoreLookupFailure;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Carries the sample MCI envelope to the PoC core-system stub. */
public final class HttpMciStubTransport implements MciProductInquiryAdapter.Transport {
    private static final String PRODUCT_INQUIRY_PATH = "/core/v1/product-inquiries";

    private final RestClient client;

    public HttpMciStubTransport(RestClient client) {
        this.client = client;
    }

    @Override
    public CoreResponse exchange(CoreRequest request) {
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
}
