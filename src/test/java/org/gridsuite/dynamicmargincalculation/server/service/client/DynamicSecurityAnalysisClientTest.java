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
import com.powsybl.contingency.Contingency;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.DynamicSecurityAnalysisParametersValues;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.gridsuite.computation.service.AbstractResultContext.VARIANT_ID_HEADER;
import static org.gridsuite.dynamicmargincalculation.server.service.client.DynamicSecurityAnalysisClient.API_VERSION;
import static org.gridsuite.dynamicmargincalculation.server.service.client.DynamicSecurityAnalysisClient.DYNAMIC_SECURITY_ANALYSIS_END_POINT_PARAMETERS;
import static org.gridsuite.dynamicmargincalculation.server.service.client.utils.UrlUtils.buildEndPointUrl;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
class DynamicSecurityAnalysisClientTest extends AbstractRestClientTest {

    private DynamicSecurityAnalysisClient dynamicSecurityAnalysisClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    void init() {
        // use new WireMockServer(DYNAMIC_SECURITY_ANALYSIS_PORT) to test with local server if needed
        dynamicSecurityAnalysisClient = new DynamicSecurityAnalysisClient(
                initMockWebServer(new WireMockServer(wireMockConfig().dynamicPort())),
                restTemplate,
                objectMapper
        );
    }

    @Test
    void testGetParametersValues() throws Exception {

        // --- Setup --- //
        UUID parametersUuid = UUID.fromString("f1be5de3-b8e5-4ab5-9521-62a1f8a66228");
        UUID networkUuid = UUID.fromString("f1be5de3-b8e5-4ab5-9521-62a1f8a66228");
        String variantId = "variant_1";

        DynamicSecurityAnalysisParametersValues expected = DynamicSecurityAnalysisParametersValues.builder()
                .contingenciesStartTime(105d)
                .contingencies(List.of(Contingency.load("_LOAD__11_EC")))
                .build();
        String bodyJson = objectMapper.writeValueAsString(expected);

        String baseEndpoint = buildEndPointUrl("", API_VERSION, DYNAMIC_SECURITY_ANALYSIS_END_POINT_PARAMETERS);
        String urlPath = baseEndpoint + "/" + parametersUuid + "/values";

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(urlPath))
                .withQueryParam("networkUuid", equalTo(networkUuid.toString()))
                .withQueryParam(VARIANT_ID_HEADER, equalTo(variantId))
                .willReturn(WireMock.ok()
                        .withBody(bodyJson)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                ));

        // --- Execute --- //
        DynamicSecurityAnalysisParametersValues result =
                dynamicSecurityAnalysisClient.getParametersValues(parametersUuid, networkUuid, variantId);

        // --- Verify --- //
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void testGetParametersValuesGivenException() {

        // --- Setup --- //
        UUID parametersUuid = UUID.fromString("eb4a62e4-0927-4e1e-9256-56a8494b0786");
        UUID networkUuid = UUID.fromString("eb4a62e4-0927-4e1e-9256-56a8494b0786");
        String variantId = "variant_1";

        String baseEndpoint = buildEndPointUrl("", API_VERSION, DYNAMIC_SECURITY_ANALYSIS_END_POINT_PARAMETERS);
        String urlPath = baseEndpoint + "/" + parametersUuid + "/values";

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(urlPath))
                .withQueryParam("networkUuid", equalTo(networkUuid.toString()))
                .withQueryParam(VARIANT_ID_HEADER, equalTo(variantId))
                .willReturn(WireMock.serverError()
                        .withBody(ERROR_MESSAGE_JSON)
                ));

        // --- Execute --- //
        HttpServerErrorException exception = catchThrowableOfType(HttpServerErrorException.class,
                () -> dynamicSecurityAnalysisClient.getParametersValues(parametersUuid, networkUuid, variantId));

        // --- Verify --- //
        assertThat(exception.getMessage()).contains(ERROR_MESSAGE);
    }
}
