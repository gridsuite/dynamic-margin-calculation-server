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
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.parameters.Parameter;
import com.powsybl.dynawo.parameters.ParameterType;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.dynawo.suppliers.PropertyBuilder;
import com.powsybl.dynawo.suppliers.PropertyType;
import com.powsybl.dynawo.suppliers.SetGroupType;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.DynamicSimulationParametersValues;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.gridsuite.computation.service.AbstractResultContext.VARIANT_ID_HEADER;
import static org.gridsuite.dynamicmargincalculation.server.service.client.DynamicSimulationClient.API_VERSION;
import static org.gridsuite.dynamicmargincalculation.server.service.client.DynamicSimulationClient.DYNAMIC_SIMULATION_END_POINT_PARAMETERS;
import static org.gridsuite.dynamicmargincalculation.server.service.client.utils.UrlUtils.buildEndPointUrl;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
class DynamicSimulationClientTest extends AbstractRestClientTest {

    private DynamicSimulationClient dynamicSimulationClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    void init() {
        // use new WireMockServer(DYNAMIC_SIMULATION_PORT) to test with local server if needed
        dynamicSimulationClient = new DynamicSimulationClient(
                initMockWebServer(new WireMockServer(wireMockConfig().dynamicPort())),
                restTemplate,
                objectMapper
        );
    }

    @Test
    void testGetParametersValues() throws Exception {

        // --- Setup --- //
        UUID networkUuid = UUID.fromString("e6516424-21c3-4bff-953d-9d0efc20ea08");
        String variantId = "variant_1";
        String dynamicSimulationParametersJson = "{}";

        // prepare dynamic model
        List<DynamicModelConfig> dynamicModel = List.of(new DynamicModelConfig("LoadAlphaBeta", "_DM", SetGroupType.SUFFIX, List.of(
                new PropertyBuilder()
                        .name("staticId")
                        .value("LOAD")
                        .type(PropertyType.STRING)
                        .build())));

        // prepare dynawo simulation parameters
        DynawoSimulationParameters dynawoParameters = DynawoSimulationParameters.load();

        // network
        ParametersSet networkParamsSet = new ParametersSet("network");
        networkParamsSet.addParameter(new Parameter("LoadBeta", ParameterType.DOUBLE, "2"));
        networkParamsSet.addParameter(new Parameter("LoadAlpha", ParameterType.DOUBLE, "1"));

        dynawoParameters.setNetworkParameters(networkParamsSet);

        // model parameters set
        dynawoParameters.setModelsParameters(List.of(new ParametersSet("LoadAlphaBeta")));

        // solver parameters set
        ParametersSet simSolverParamsSet = new ParametersSet("SIM");
        simSolverParamsSet.addParameter(new Parameter("HMin", ParameterType.DOUBLE, "0.001"));
        simSolverParamsSet.addParameter(new Parameter("LinearSolverName", ParameterType.STRING, "KLU"));

        dynawoParameters.setSolverParameters(simSolverParamsSet);

        // build Dto to mock return
        DynamicSimulationParametersValues expected = DynamicSimulationParametersValues.builder()
                .dynamicModel(dynamicModel)
                .dynawoParameters(dynawoParameters)
                .build();

        String bodyJson = objectMapper.writeValueAsString(expected);

        String baseEndpoint = buildEndPointUrl("", API_VERSION, DYNAMIC_SIMULATION_END_POINT_PARAMETERS);
        String urlPath = baseEndpoint + "/values";

        wireMockServer.stubFor(WireMock.post(WireMock.urlPathEqualTo(urlPath))
                .withQueryParam("networkUuid", equalTo(networkUuid.toString()))
                .withQueryParam(VARIANT_ID_HEADER, equalTo(variantId))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withRequestBody(WireMock.equalToJson(dynamicSimulationParametersJson))
                .willReturn(WireMock.ok()
                        .withBody(bodyJson)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                ));

        // --- Execute --- //
        DynamicSimulationParametersValues result =
                dynamicSimulationClient.getParametersValues(dynamicSimulationParametersJson, networkUuid, variantId);

        // --- Verify --- //
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void testGetParametersValuesGivenException() {

        // --- Setup --- //
        UUID networkUuid = UUID.fromString("4b364ee1-2933-401a-ae3c-e79253b2de67");
        String variantId = "variant_1";
        String dynamicSimulationParametersJson = "{}";

        String baseEndpoint = buildEndPointUrl("", API_VERSION, DYNAMIC_SIMULATION_END_POINT_PARAMETERS);
        String urlPath = baseEndpoint + "/values";

        wireMockServer.stubFor(WireMock.post(WireMock.urlPathEqualTo(urlPath))
                .withQueryParam("networkUuid", equalTo(networkUuid.toString()))
                .withQueryParam(VARIANT_ID_HEADER, equalTo(variantId))
                .willReturn(WireMock.serverError()
                        .withBody(ERROR_MESSAGE_JSON)
                ));

        // --- Execute --- //
        HttpServerErrorException exception = catchThrowableOfType(HttpServerErrorException.class,
                () -> dynamicSimulationClient.getParametersValues(dynamicSimulationParametersJson, networkUuid, variantId));

        // --- Verify --- //
        assertThat(exception.getMessage()).contains(ERROR_MESSAGE);
    }
}
