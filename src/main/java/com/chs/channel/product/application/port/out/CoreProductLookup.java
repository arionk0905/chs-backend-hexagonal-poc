package com.chs.channel.product.application.port.out;

import com.chs.channel.product.domain.Product;
import com.chs.channel.product.domain.ProductCode;
import java.util.Optional;

public interface CoreProductLookup {
    /**
     * Returns empty when the product is missing.
     * @throws CoreLookupFailure when a lookup cannot be completed
     */
    Optional<Product> find(ProductCode code);
}
