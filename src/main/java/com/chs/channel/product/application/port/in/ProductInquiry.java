package com.chs.channel.product.application.port.in;

import com.chs.channel.product.domain.Product;
import com.chs.channel.product.domain.ProductCode;
import java.util.Optional;

public interface ProductInquiry {
    Optional<Product> inquire(ProductCode code);
}
