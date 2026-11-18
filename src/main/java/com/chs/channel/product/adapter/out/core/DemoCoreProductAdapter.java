package com.chs.channel.product.adapter.out.core;

import com.chs.channel.product.application.port.out.CoreProductLookup;
import com.chs.channel.product.domain.Product;
import com.chs.channel.product.domain.ProductCode;
import java.util.Optional;

public final class DemoCoreProductAdapter implements CoreProductLookup {
    private static final Product DEMO_PRODUCT = new Product(new ProductCode("SAMPLE-DEPOSIT"), "POC 예금 상품");

    @Override
    public Optional<Product> find(ProductCode code) {
        return DEMO_PRODUCT.code().equals(code) ? Optional.of(DEMO_PRODUCT) : Optional.empty();
    }
}
