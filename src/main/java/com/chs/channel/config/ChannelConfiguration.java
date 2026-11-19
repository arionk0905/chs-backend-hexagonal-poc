package com.chs.channel.config;

import com.chs.channel.product.adapter.out.core.DemoCoreProductAdapter;
import com.chs.channel.product.adapter.out.core.http.CoreProductHttpAdapter;
import com.chs.channel.product.application.ProductInquiryService;
import com.chs.channel.product.application.port.in.ProductInquiry;
import com.chs.channel.product.application.port.out.CoreProductLookup;
import com.chs.channel.request.adapter.out.mariadb.MariaDbChannelRequestAdapter;
import com.chs.channel.request.application.ChannelRequestService;
import com.chs.channel.request.application.port.in.ChannelRequests;
import com.chs.channel.request.application.port.out.ChannelRequestStore;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class ChannelConfiguration {
    @Bean
    ProductInquiry productInquiry(CoreProductLookup core) {
        return new ProductInquiryService(core);
    }

    @Bean
    @Profile("!core-http")
    CoreProductLookup demoCore() {
        return new DemoCoreProductAdapter();
    }

    @Bean
    @Profile("core-http")
    CoreProductLookup httpCore(
            RestClient.Builder builder,
            @Value("${core.http.base-url}") String baseUrl,
            @Value("${core.http.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${core.http.read-timeout-ms}") long readTimeoutMs) {
        var transport = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMs)).build();
        var requestFactory = new JdkClientHttpRequestFactory(transport);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        var client = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        return new CoreProductHttpAdapter(client);
    }

    @Bean
    ChannelRequests channelRequests(ChannelRequestStore store) {
        // Match MariaDB DATETIME(6) so a saved timestamp round-trips through the API.
        return new ChannelRequestService(store, Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000)));
    }

    @Bean
    ChannelRequestStore channelRequestStore(JdbcClient jdbc) {
        return new MariaDbChannelRequestAdapter(jdbc);
    }
}
