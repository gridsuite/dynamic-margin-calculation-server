/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.gridsuite.dynamicmargincalculation.server.dto.ElementAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.gridsuite.computation.service.NotificationService.HEADER_USER_ID;
import static org.gridsuite.dynamicmargincalculation.server.service.client.utils.UrlUtils.buildEndPointUrl;

/**
 *
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class DirectoryClient extends AbstractRestClient {
    public static final String API_VERSION = "v1";
    public static final String ELEMENT_END_POINT_INFOS = "elements";
    public static final String QUERY_PARAM_IDS = "ids";
    public static final String QUERY_PARAM_STRICT_MODE = "strictMode";

    protected DirectoryClient(
            @Value("${gridsuite.services.directory-server.base-uri:http://directory-server/}") String baseUri,
            RestTemplate restTemplate,
            ObjectMapper objectMapper
    ) {
        super(baseUri, restTemplate, objectMapper);
    }

    public Map<UUID, String> getElementNames(List<UUID> ids, String userId) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }

        String endPointUrl = buildEndPointUrl(getBaseUri(), API_VERSION, ELEMENT_END_POINT_INFOS);

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(endPointUrl);
        uriComponentsBuilder.queryParam(QUERY_PARAM_IDS, ids);
        uriComponentsBuilder.queryParam(QUERY_PARAM_STRICT_MODE, false); // to ignore non existing elements error

        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.isNotBlank(userId)) {
            headers.set(HEADER_USER_ID, userId);
        }

        List<ElementAttributes> elementAttributes = getRestTemplate()
            .exchange(
                uriComponentsBuilder.build().toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<List<ElementAttributes>>() { }
            ).getBody();

        return elementAttributes == null ?
                Collections.emptyMap() :
                elementAttributes.stream().collect(Collectors.toMap(ElementAttributes::getElementUuid, ElementAttributes::getElementName));
    }
}
