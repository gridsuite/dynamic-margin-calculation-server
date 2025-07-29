/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.service;

import jakarta.transaction.Transactional;
import org.gridsuite.computation.service.AbstractComputationResultService;
import org.gridsuite.dynamicmargincalculation.server.DynamicMarginCalculationException;
import org.gridsuite.dynamicmargincalculation.server.dto.DynamicMarginCalculationStatus;
import org.gridsuite.dynamicmargincalculation.server.entities.DynamicMarginCalculationResultEntity;
import org.gridsuite.dynamicmargincalculation.server.repositories.DynamicMarginCalculationResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.dynamicmargincalculation.server.DynamicMarginCalculationException.Type.RESULT_UUID_NOT_FOUND;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class DynamicMarginCalculationResultService extends AbstractComputationResultService<DynamicMarginCalculationStatus> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicMarginCalculationResultService.class);

    public static final String MSG_RESULT_UUID_NOT_FOUND = "Result uuid not found: ";

    private final DynamicMarginCalculationResultRepository resultRepository;

    public DynamicMarginCalculationResultService(DynamicMarginCalculationResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    @Override
    @Transactional
    public void insertStatus(List<UUID> resultUuids, DynamicMarginCalculationStatus status) {
        Objects.requireNonNull(resultUuids);
        resultRepository.saveAll(resultUuids.stream()
            .map(uuid -> new DynamicMarginCalculationResultEntity(uuid, status)).toList());
    }

    @Transactional
    public List<UUID> updateStatus(List<UUID> resultUuids, DynamicMarginCalculationStatus status) {
        // find result entities
        List<DynamicMarginCalculationResultEntity> resultEntities = resultRepository.findAllById(resultUuids);
        // set entity with new values
        resultEntities.forEach(resultEntity -> resultEntity.setStatus(status));
        // save entities into database
        return resultRepository.saveAllAndFlush(resultEntities).stream().map(DynamicMarginCalculationResultEntity::getId).toList();
    }

    @Transactional
    public void updateResult(UUID resultUuid, DynamicMarginCalculationStatus status) {
        LOGGER.debug("Update dynamic simulation [resultUuid={}, status={}", resultUuid, status);
        DynamicMarginCalculationResultEntity resultEntity = resultRepository.findById(resultUuid)
               .orElseThrow(() -> new DynamicMarginCalculationException(RESULT_UUID_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid));
        resultEntity.setStatus(status);
    }

    @Override
    public void delete(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        resultRepository.deleteById(resultUuid);
    }

    @Override
    @Transactional
    public void deleteAll() {
        resultRepository.deleteAll();
    }

    @Override
    public DynamicMarginCalculationStatus findStatus(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid)
            .map(DynamicMarginCalculationResultEntity::getStatus)
            .orElse(null);
    }
}
