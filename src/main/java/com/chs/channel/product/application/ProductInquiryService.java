package com.chs.channel.product.application;

import com.chs.channel.product.application.port.in.ProductInquiry;
import com.chs.channel.product.application.port.out.CoreProductLookup;
import com.chs.channel.product.domain.Product;
import com.chs.channel.product.domain.ProductCode;
import java.util.Optional;

public final class ProductInquiryService implements ProductInquiry {
    private final CoreProductLookup core;

    public ProductInquiryService(CoreProductLookup core) {
        this.core = core;
    }

    @Override
    public Optional<Product> inquire(ProductCode code) {
        return core.find(code);
    }
}
