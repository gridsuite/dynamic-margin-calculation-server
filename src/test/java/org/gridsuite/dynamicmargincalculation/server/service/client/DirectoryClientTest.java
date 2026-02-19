/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.gridsuite.dynamicmargincalculation.server.dto.ElementAttributes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.havingExactly;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.gridsuite.computation.service.NotificationService.HEADER_USER_ID;
import static org.gridsuite.dynamicmargincalculation.server.service.client.DirectoryClient.*;
import static org.gridsuite.dynamicmargincalculation.server.service.client.utils.UrlUtils.buildEndPointUrl;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
class DirectoryClientTest extends AbstractRestClientTest {

    private DirectoryClient directoryClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String getBaseUrl() {
        return buildEndPointUrl("", API_VERSION, ELEMENT_END_POINT_INFOS);
    }

    @BeforeAll
    void init() {
        directoryClient = new DirectoryClient(
                // use new WireMockServer(DIRECTORY_PORT) to test with local server if needed
                initMockWebServer(new WireMockServer(wireMockConfig().dynamicPort())),
                restTemplate,
                objectMapper
        );
    }

    @Test
    void testGetElementNamesEmptyInput() {
        Map<UUID, String> result = directoryClient.getElementNames(List.of(), "userId");
        assertThat(result).isEmpty();
    }

    @Test
    void testGetElementNames() throws Exception {

        // --- Setup --- //
        UUID id1 = UUID.fromString("0d740ec6-15ca-4d81-9963-ffed732c3d36");
        UUID id2 = UUID.fromString("ad1f5f93-67ad-4ea8-a85a-b82a52cce0a2");
        String userId = "testUserId";

        List<ElementAttributes> responseBody = List.of(
                new ElementAttributes(id1, "Element 1"),
                new ElementAttributes(id2, "Element 2")
        );

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(getBaseUrl()))
                .withQueryParam(QUERY_PARAM_IDS, havingExactly(id1.toString(), id2.toString()))
                .withQueryParam(QUERY_PARAM_STRICT_MODE, equalTo("false"))
                .withHeader(HEADER_USER_ID, equalTo(userId))
                .willReturn(WireMock.ok()
                        .withBody(objectMapper.writeValueAsString(responseBody))
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                ));

        // --- Execute --- //
        Map<UUID, String> result = directoryClient.getElementNames(List.of(id1, id2), userId);

        // --- Verify --- //
        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                id1, "Element 1",
                id2, "Element 2"
        ));
    }

    @Test
    void testGetElementNamesGivenException() {

        // --- Setup --- //
        UUID id1 = UUID.fromString("a5efc25c-c836-423d-a8ba-0c6e74a2f8ae");

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(getBaseUrl()))
                .withQueryParam(QUERY_PARAM_IDS, havingExactly(id1.toString()))
                .withQueryParam(QUERY_PARAM_STRICT_MODE, equalTo("false"))
                .willReturn(WireMock.serverError()
                        .withBody(ERROR_MESSAGE_JSON)
                ));

        // --- Execute --- //
        HttpServerErrorException exception = catchThrowableOfType(HttpServerErrorException.class,
                () -> directoryClient.getElementNames(List.of(id1), null));

        // --- Verify --- //
        assertThat(exception.getMessage()).contains(ERROR_MESSAGE);
    }
}
