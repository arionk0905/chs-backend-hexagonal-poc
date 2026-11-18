package com.chs.channel.product.adapter.in.web;

import com.chs.channel.product.application.port.in.ProductInquiry;
import com.chs.channel.product.application.port.out.CoreLookupFailure;
import com.chs.channel.product.domain.ProductCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/core/product-inquiries")
public class ProductInquiryController {
    private final ProductInquiry inquiry;

    public ProductInquiryController(ProductInquiry inquiry) {
        this.inquiry = inquiry;
    }

    @PostMapping
    ProductResponse inquire(@RequestBody InquiryRequest request) {
        ProductCode code;
        try {
            code = new ProductCode(request.productCode());
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error);
        }
        return inquiry.inquire(code)
                .map(product -> new ProductResponse(product.code().value(), product.name()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

    @ExceptionHandler(CoreLookupFailure.class)
    ProblemDetail handleCoreFailure(CoreLookupFailure error) {
        return switch (error.kind()) {
            case TIMEOUT -> ProblemDetail.forStatusAndDetail(
                    HttpStatus.GATEWAY_TIMEOUT, "계정계 응답 시간이 초과됐습니다.");
            case UNAVAILABLE -> ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_GATEWAY, "계정계에 연결할 수 없습니다.");
            case INVALID_RESPONSE -> ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_GATEWAY, "계정계 응답을 처리할 수 없습니다.");
        };
    }

    record InquiryRequest(String productCode) {}
    record ProductResponse(String productCode, String productName) {}
}
