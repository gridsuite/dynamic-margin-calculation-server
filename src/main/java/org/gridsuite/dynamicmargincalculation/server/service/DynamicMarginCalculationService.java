/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicmargincalculation.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.dynawo.margincalculation.MarginCalculation;
import com.powsybl.network.store.client.NetworkStoreService;
import org.gridsuite.computation.s3.ComputationS3Service;
import org.gridsuite.computation.service.AbstractComputationService;
import org.gridsuite.computation.service.NotificationService;
import org.gridsuite.computation.service.UuidGeneratorService;
import org.gridsuite.dynamicmargincalculation.server.dto.DynamicMarginCalculationStatus;
import org.gridsuite.dynamicmargincalculation.server.service.contexts.DynamicMarginCalculationResultContext;
import org.gridsuite.dynamicmargincalculation.server.service.contexts.DynamicMarginCalculationRunContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
@ComponentScan(basePackageClasses = {NetworkStoreService.class, NotificationService.class})
public class DynamicMarginCalculationService extends AbstractComputationService<DynamicMarginCalculationRunContext, DynamicMarginCalculationResultService, DynamicMarginCalculationStatus> {
    public static final String COMPUTATION_TYPE = "dynamic margin calculation";

    public DynamicMarginCalculationService(
            NotificationService notificationService,
            ObjectMapper objectMapper,
            UuidGeneratorService uuidGeneratorService,
            DynamicMarginCalculationResultService dynamicSecurityAnalysisResultService,
            ComputationS3Service computationS3Service,
            @Value("${dynamic-margin-calculation.default-provider}") String defaultProvider) {
        super(notificationService, dynamicSecurityAnalysisResultService, computationS3Service, objectMapper, uuidGeneratorService, defaultProvider);
    }

    @Override
    public UUID runAndSaveResult(DynamicMarginCalculationRunContext runContext) {
        // insert a new result entity with running status
        UUID resultUuid = uuidGeneratorService.generate();
        resultService.insertStatus(List.of(resultUuid), DynamicMarginCalculationStatus.RUNNING);

        // emit a message to launch the dynamic security analysis by the worker service
        Message<String> message = new DynamicMarginCalculationResultContext(resultUuid, runContext).toMessage(objectMapper);
        notificationService.sendRunMessage(message);
        return resultUuid;
    }

    public List<String> getProviders() {
        return List.of(MarginCalculation.getRunner().getName());
    }
}
