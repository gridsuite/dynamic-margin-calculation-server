/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.service;

import org.gridsuite.dynamicmargincalculation.server.DynamicMarginCalculationException;
import org.gridsuite.dynamicmargincalculation.server.dto.DynamicMarginCalculationStatus;
import org.gridsuite.dynamicmargincalculation.server.entities.DynamicMarginCalculationResultEntity;
import org.gridsuite.dynamicmargincalculation.server.repositories.DynamicMarginCalculationResultRepository;
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
    DynamicMarginCalculationResultRepository resultRepository;

    @Autowired
    DynamicMarginCalculationResultService dynamicMarginCalculationResultService;

    @AfterEach
    void cleanDB() {
        resultRepository.deleteAll();
    }

    @Test
    void testCrud() {
        // --- insert an entity in the db --- //
        LOGGER.info("Test insert status");
        UUID resultUuid = UUID.randomUUID();
        dynamicMarginCalculationResultService.insertStatus(List.of(resultUuid), DynamicMarginCalculationStatus.SUCCEED);

        Optional<DynamicMarginCalculationResultEntity> insertedResultEntityOpt = resultRepository.findById(resultUuid);
        assertThat(insertedResultEntityOpt).isPresent();
        LOGGER.info("Expected result status = {}", DynamicMarginCalculationStatus.SUCCEED);
        LOGGER.info("Actual inserted result status = {}", insertedResultEntityOpt.get().getStatus());
        assertThat(insertedResultEntityOpt.get().getStatus()).isSameAs(DynamicMarginCalculationStatus.SUCCEED);

        // --- get status of the entity -- //
        LOGGER.info("Test find status");
        DynamicMarginCalculationStatus status = dynamicMarginCalculationResultService.findStatus(resultUuid);

        LOGGER.info("Expected result status = {}", DynamicMarginCalculationStatus.SUCCEED);
        LOGGER.info("Actual get result status = {}", insertedResultEntityOpt.get().getStatus());
        assertThat(status).isEqualTo(DynamicMarginCalculationStatus.SUCCEED);

        // --- update the entity --- //
        LOGGER.info("Test update status");
        List<UUID> updatedResultUuids = dynamicMarginCalculationResultService.updateStatus(List.of(resultUuid), DynamicMarginCalculationStatus.NOT_DONE);

        Optional<DynamicMarginCalculationResultEntity> updatedResultEntityOpt = resultRepository.findById(updatedResultUuids.getFirst());

        // status must be changed
        assertThat(updatedResultEntityOpt).isPresent();
        LOGGER.info("Expected result status = {}", DynamicMarginCalculationStatus.NOT_DONE);
        LOGGER.info("Actual updated result status = {}", updatedResultEntityOpt.get().getStatus());
        assertThat(updatedResultEntityOpt.get().getStatus()).isSameAs(DynamicMarginCalculationStatus.NOT_DONE);

        // --- update entity with non-existing UUID --- //
        LOGGER.info("Test update status with non-existing UUID");
        UUID nonExistingUuid = UUID.randomUUID();

        // should throw DynamicMarginCalculationException since the UUID doesn't exist
        assertThatThrownBy(() -> dynamicMarginCalculationResultService.updateResult(nonExistingUuid, DynamicMarginCalculationStatus.FAILED))
                .isInstanceOf(DynamicMarginCalculationException.class)
                .hasMessageContaining("Result uuid not found: " + nonExistingUuid);

        LOGGER.info("Non-existing UUID update threw expected exception");

        // --- delete result --- //
        LOGGER.info("Test delete a result");
        dynamicMarginCalculationResultService.delete(resultUuid);

        Optional<DynamicMarginCalculationResultEntity> foundResultEntity = resultRepository.findById(resultUuid);
        assertThat(foundResultEntity).isNotPresent();

        // --- get status of a deleted entity --- //
        status = dynamicMarginCalculationResultService.findStatus(resultUuid);
        assertThat(status).isNull();

        // --- delete all --- //
        LOGGER.info("Test delete all results");
        resultRepository.saveAllAndFlush(List.of(
                new DynamicMarginCalculationResultEntity(UUID.randomUUID(), DynamicMarginCalculationStatus.RUNNING),
                new DynamicMarginCalculationResultEntity(UUID.randomUUID(), DynamicMarginCalculationStatus.RUNNING)
        ));

        dynamicMarginCalculationResultService.deleteAll();
        assertThat(resultRepository.findAll()).isEmpty();
    }
}
