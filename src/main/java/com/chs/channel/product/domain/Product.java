package com.chs.channel.product.domain;

public record Product(ProductCode code, String name) {
    public Product {
        if (code == null || name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품 코드와 이름이 필요합니다.");
        }
    }
}
