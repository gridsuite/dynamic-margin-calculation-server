package org.gridsuite.dynamicmargincalculation.server.service;

import com.powsybl.network.store.client.NetworkStoreService;
import org.gridsuite.computation.service.AbstractFilterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

@Service
public class FilterService extends AbstractFilterService {

    public FilterService(RestTemplateBuilder restTemplateBuilder,
                         NetworkStoreService networkStoreService,
                         @Value("${gridsuite.services.filter-server.base-uri:http://filter-server/}") String filterServerBaseUri) {
        super(restTemplateBuilder, networkStoreService, filterServerBaseUri);
    }
}
