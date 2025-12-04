/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.service;

import jakarta.transaction.Transactional;
import org.gridsuite.computation.error.ComputationException;
import org.gridsuite.computation.service.AbstractComputationResultService;
import org.gridsuite.dynamicmargincalculation.server.dto.DynamicMarginCalculationStatus;
import org.gridsuite.dynamicmargincalculation.server.entities.DynamicMarginCalculationStatusEntity;
import org.gridsuite.dynamicmargincalculation.server.repositories.DynamicMarginCalculationStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.computation.error.ComputationBusinessErrorCode.RESULT_NOT_FOUND;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class DynamicMarginCalculationResultService extends AbstractComputationResultService<DynamicMarginCalculationStatus> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicMarginCalculationResultService.class);

    public static final String MSG_RESULT_UUID_NOT_FOUND = "Result uuid not found: ";

    private final DynamicMarginCalculationStatusRepository statusRepository;

    public DynamicMarginCalculationResultService(DynamicMarginCalculationStatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    @Override
    @Transactional
    public void insertStatus(List<UUID> resultUuids, DynamicMarginCalculationStatus status) {
        Objects.requireNonNull(resultUuids);
        statusRepository.saveAll(resultUuids.stream()
            .map(uuid -> new DynamicMarginCalculationStatusEntity(uuid, status)).toList());
    }

    @Transactional
    public List<UUID> updateStatus(List<UUID> resultUuids, DynamicMarginCalculationStatus status) {
        // find status entities
        List<DynamicMarginCalculationStatusEntity> resultEntities = statusRepository.findAllById(resultUuids);
        // set entity with new values
        resultEntities.forEach(resultEntity -> resultEntity.setStatus(status));
        // save entities into database
        return statusRepository.saveAllAndFlush(resultEntities).stream().map(DynamicMarginCalculationStatusEntity::getResultUuid).toList();
    }

    @Transactional
    public void updateStatus(UUID resultUuid, DynamicMarginCalculationStatus status) {
        LOGGER.debug("Update margin calculation status [resultUuid={}, status={}", resultUuid, status);
        DynamicMarginCalculationStatusEntity resultEntity = statusRepository.findByResultUuid(resultUuid)
               .orElseThrow(() -> new ComputationException(RESULT_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid));
        resultEntity.setStatus(status);
    }

    @Override
    @Transactional
    public void delete(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        statusRepository.deleteByResultUuid(resultUuid);
    }

    @Override
    @Transactional
    public void deleteAll() {
        statusRepository.deleteAll();
    }

    @Override
    public DynamicMarginCalculationStatus findStatus(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return statusRepository.findByResultUuid(resultUuid)
            .map(DynamicMarginCalculationStatusEntity::getStatus)
            .orElse(null);
    }
}
