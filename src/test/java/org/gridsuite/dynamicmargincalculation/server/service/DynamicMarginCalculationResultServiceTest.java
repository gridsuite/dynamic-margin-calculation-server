/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.service;

import org.gridsuite.dynamicmargincalculation.server.DynamicMarginCalculationException;
import org.gridsuite.dynamicmargincalculation.server.dto.DynamicMarginCalculationStatus;
import org.gridsuite.dynamicmargincalculation.server.entities.DynamicMarginCalculationStatusEntity;
import org.gridsuite.dynamicmargincalculation.server.repositories.DynamicMarginCalculationStatusRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@SpringBootTest
class DynamicMarginCalculationResultServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicMarginCalculationResultServiceTest.class);

    @Autowired
    DynamicMarginCalculationStatusRepository statusRepository;

    @Autowired
    DynamicMarginCalculationResultService dynamicMarginCalculationResultService;

    @AfterEach
    void cleanDB() {
        statusRepository.deleteAll();
    }

    @Test
    void testCrudStatus() {
        // --- insert a status in the db --- //
        LOGGER.info("Test insert status");
        UUID resultUuid = UUID.randomUUID();
        dynamicMarginCalculationResultService.insertStatus(List.of(resultUuid), DynamicMarginCalculationStatus.SUCCEED);

        Optional<DynamicMarginCalculationStatusEntity> insertedResultEntityOpt = statusRepository.findByResultUuid(resultUuid);
        assertThat(insertedResultEntityOpt).isPresent();
        LOGGER.info("Expected result status = {}", DynamicMarginCalculationStatus.SUCCEED);
        LOGGER.info("Actual inserted result status = {}", insertedResultEntityOpt.get().getStatus());
        assertThat(insertedResultEntityOpt.get().getStatus()).isSameAs(DynamicMarginCalculationStatus.SUCCEED);

        // --- get status -- //
        LOGGER.info("Test find status");
        DynamicMarginCalculationStatus status = dynamicMarginCalculationResultService.findStatus(resultUuid);

        LOGGER.info("Expected result status = {}", DynamicMarginCalculationStatus.SUCCEED);
        LOGGER.info("Actual get result status = {}", insertedResultEntityOpt.get().getStatus());
        assertThat(status).isEqualTo(DynamicMarginCalculationStatus.SUCCEED);

        // --- update status --- //
        LOGGER.info("Test update status");
        List<UUID> updatedResultUuids = dynamicMarginCalculationResultService.updateStatus(List.of(resultUuid), DynamicMarginCalculationStatus.NOT_DONE);

        Optional<DynamicMarginCalculationStatusEntity> updatedResultEntityOpt = statusRepository.findByResultUuid(updatedResultUuids.getFirst());

        // status must be changed
        assertThat(updatedResultEntityOpt).isPresent();
        LOGGER.info("Expected result status = {}", DynamicMarginCalculationStatus.NOT_DONE);
        LOGGER.info("Actual updated result status = {}", updatedResultEntityOpt.get().getStatus());
        assertThat(updatedResultEntityOpt.get().getStatus()).isSameAs(DynamicMarginCalculationStatus.NOT_DONE);

        // --- update status entity with non-existing UUID --- //
        LOGGER.info("Test update status with non-existing UUID");
        UUID nonExistingUuid = UUID.randomUUID();

        // should throw DynamicMarginCalculationException since the UUID doesn't exist
        assertThatThrownBy(() -> dynamicMarginCalculationResultService.updateStatus(nonExistingUuid, DynamicMarginCalculationStatus.FAILED))
                .isInstanceOf(DynamicMarginCalculationException.class)
                .hasMessageContaining("Result uuid not found: " + nonExistingUuid);

        LOGGER.info("Non-existing UUID update threw expected exception");

        // --- delete status --- //
        LOGGER.info("Test delete a status");
        dynamicMarginCalculationResultService.delete(resultUuid);

        Optional<DynamicMarginCalculationStatusEntity> foundResultEntity = statusRepository.findByResultUuid(resultUuid);
        assertThat(foundResultEntity).isNotPresent();

        // --- get the status of a deleted status entity --- //
        status = dynamicMarginCalculationResultService.findStatus(resultUuid);
        assertThat(status).isNull();

        // --- delete all --- //
        LOGGER.info("Test delete all results");
        statusRepository.saveAllAndFlush(List.of(
                new DynamicMarginCalculationStatusEntity(UUID.randomUUID(), DynamicMarginCalculationStatus.RUNNING),
                new DynamicMarginCalculationStatusEntity(UUID.randomUUID(), DynamicMarginCalculationStatus.RUNNING)
        ));

        dynamicMarginCalculationResultService.deleteAll();
        assertThat(statusRepository.findAll()).isEmpty();
    }
}
