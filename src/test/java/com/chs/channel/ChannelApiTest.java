package com.chs.channel;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MariaDbTestConfiguration.class)
class ChannelApiTest {
    private static final String REQUESTS = "/api/v1/channel-requests";
    private static final String INQUIRIES = "/api/v1/core/product-inquiries";

    @Autowired
    MockMvc mvc;

    @Test
    void storesRequestAndReadsItFromMariaDb() throws Exception {
        var created = mvc.perform(post(REQUESTS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"  채널 테스트  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.createdAt").isString())
                .andReturn().getResponse();
        String location = created.getHeader("Location");
        assertNotNull(location);
        String createdAt = JsonMapper.builder().build().readTree(created.getContentAsByteArray())
                .path("createdAt").asString();

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("채널 테스트"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.createdAt").value(createdAt));
    }

    @Test
    void rejectsInvalidInputAndHandlesUnknownRequests() throws Exception {
        mvc.perform(post(REQUESTS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"  \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(REQUESTS + "/invalid-uuid")).andExpect(status().isBadRequest());
        mvc.perform(get(REQUESTS + "/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void usesDemoAdapterByDefault() throws Exception {
        mvc.perform(post(INQUIRIES).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"SAMPLE-DEPOSIT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("POC 예금 상품"))
                .andExpect(jsonPath("$.header").doesNotExist());
        mvc.perform(post(INQUIRIES).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"UNKNOWN\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post(INQUIRIES).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"invalid code\"}"))
                .andExpect(status().isBadRequest());
    }
}
