/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.DynamicSimulationParametersValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

import static org.gridsuite.computation.service.AbstractResultContext.VARIANT_ID_HEADER;
import static org.gridsuite.dynamicmargincalculation.server.service.client.utils.UrlUtils.buildEndPointUrl;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class DynamicSimulationClient extends AbstractRestClient {

    public static final String API_VERSION = "v1";
    public static final String DYNAMIC_SIMULATION_REST_API_CALLED_SUCCESSFULLY_MESSAGE = "Dynamic simulation REST API called successfully {}";

    public static final String DYNAMIC_SIMULATION_END_POINT_PARAMETERS = "parameters";

    @Autowired
    public DynamicSimulationClient(@Value("${gridsuite.services.dynamic-simulation-server.base-uri:http://dynamic-simulation-server/}") String baseUri,
                                   RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(baseUri, restTemplate, objectMapper);
    }

    public DynamicSimulationParametersValues getParametersValues(String dynamicSimulationParametersJson, UUID networkUuid, String variant) {
        String endPointUrl = buildEndPointUrl(getBaseUri(), API_VERSION, DYNAMIC_SIMULATION_END_POINT_PARAMETERS);

        // TODO should use GET instead of POST after moving dynamic simulation parameters to its server
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(endPointUrl + "/values")
                .queryParam("networkUuid", networkUuid)
                .queryParam(VARIANT_ID_HEADER, variant)
                .build();

        // call dynamic simulation REST API
        String url = uriComponents.toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(dynamicSimulationParametersJson, headers);

        ResponseEntity<DynamicSimulationParametersValues> result = getRestTemplate().exchange(url, HttpMethod.POST, requestEntity, DynamicSimulationParametersValues.class);

        logger.debug(DYNAMIC_SIMULATION_REST_API_CALLED_SUCCESSFULLY_MESSAGE, url);
        return result.getBody();
    }
}
