package com.chs.channel.product.domain;

import java.util.regex.Pattern;

public record ProductCode(String value) {
    private static final Pattern FORMAT = Pattern.compile("[A-Z0-9_-]{1,20}");

    public ProductCode {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("상품 코드는 영문 대문자, 숫자, _, -로 1~20자여야 합니다.");
        }
    }
}
