/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.DynamicSecurityAnalysisParametersValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class DynamicSecurityAnalysisClient extends AbstractRestClient {

    public static final String API_VERSION = "v1";
    public static final String DYNAMIC_SECURITY_ANALYSIS_REST_API_CALLED_SUCCESSFULLY_MESSAGE = "Dynamic security analysis REST API called successfully {}";

    public static final String DYNAMIC_SECURITY_ANALYSIS_END_POINT_PARAMETERS = "parameters";

    @Autowired
    public DynamicSecurityAnalysisClient(@Value("${gridsuite.services.dynamic-security-analysis-server.base-uri:http://dynamic-security-analysis-server/}") String baseUri,
                                         RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(baseUri, restTemplate, objectMapper);
    }

    public DynamicSecurityAnalysisParametersValues getParametersValues(UUID dynamicSecurityAnalysisParametersUuid, UUID networkUuid, String variant) {
        String endPointUrl = buildEndPointUrl(getBaseUri(), API_VERSION, DYNAMIC_SECURITY_ANALYSIS_END_POINT_PARAMETERS);

        UriComponents uriComponents = UriComponentsBuilder.fromUriString(endPointUrl + "/{parametersUuid}/values")
                .queryParam("networkUuid", networkUuid)
                .queryParam(VARIANT_ID_HEADER, variant)
                .buildAndExpand(dynamicSecurityAnalysisParametersUuid);

        // call dynamic security analysis REST API
        String url = uriComponents.toUriString();
        DynamicSecurityAnalysisParametersValues result = getRestTemplate().getForObject(url, DynamicSecurityAnalysisParametersValues.class);
        logger.debug(DYNAMIC_SECURITY_ANALYSIS_REST_API_CALLED_SUCCESSFULLY_MESSAGE, url);
        return result;
    }

}
