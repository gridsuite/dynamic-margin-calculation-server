/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicmargincalculation.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.network.store.client.NetworkStoreService;
import lombok.SneakyThrows;
import org.gridsuite.computation.service.ReportService;
import org.gridsuite.dynamicmargincalculation.server.DynamicMarginCalculationApplication;
import org.gridsuite.dynamicmargincalculation.server.controller.utils.TestUtils;
import org.gridsuite.dynamicmargincalculation.server.dto.DynamicMarginCalculationStatus;
import org.gridsuite.dynamicmargincalculation.server.repositories.DynamicMarginCalculationParametersRepository;
import org.gridsuite.dynamicmargincalculation.server.service.DynamicMarginCalculationWorkerService;
import org.gridsuite.dynamicmargincalculation.server.service.FilterService;
import org.gridsuite.dynamicmargincalculation.server.service.ParametersService;
import org.gridsuite.dynamicmargincalculation.server.service.client.DirectoryClient;
import org.gridsuite.dynamicmargincalculation.server.service.client.DynamicSecurityAnalysisClient;
import org.gridsuite.dynamicmargincalculation.server.service.client.DynamicSimulationClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared setup for DynamicMarginCalculationController integration tests (MockMvc + test binder + Dynawo computation manager).
 *
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest
@ContextConfiguration(classes = {DynamicMarginCalculationApplication.class, TestChannelBinderConfiguration.class})
public abstract class AbstractDynamicMarginCalculationControllerTest extends AbstractDynawoTest {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final String dmcDebugDestination = "dmc.debug.destination";
    protected final String dmcResultDestination = "dmc.result.destination";
    protected final String dmcStoppedDestination = "dmc.stopped.destination";
    protected final String dmcCancelFailedDestination = "dmc.cancelfailed.destination";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected ReportService reportService;

    @MockitoBean
    protected DirectoryClient directoryClient;

    @MockitoBean
    protected FilterService filterClient;

    @MockitoBean
    protected DynamicSecurityAnalysisClient dynamicSecurityAnalysisClient;

    @MockitoBean
    protected DynamicSimulationClient dynamicSimulationClient;

    @MockitoBean
    protected NetworkStoreService networkStoreClient;

    @MockitoBean
    protected DynamicMarginCalculationParametersRepository dynamicMarginCalculationParametersRepository;

    @MockitoSpyBean
    protected ParametersService parametersService;

    @MockitoSpyBean
    protected DynamicMarginCalculationWorkerService dynamicMarginCalculationWorkerService;

    @BeforeEach
    @Override
    public void setUp() throws IOException {
        super.setUp();
        initDynamicMarginCalculationWorkerServiceSpy();
        initParametersRepositoryMock();
        initExternalClientsMocks();
    }

    @SneakyThrows
    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();

        // delete all results
        mockMvc.perform(delete("/v1/results"))
                .andExpect(status().isOk());

        // ensure queues are empty then clear
        OutputDestination output = getOutputDestination();
        List<String> destinations = List.of(dmcDebugDestination, dmcResultDestination, dmcStoppedDestination, dmcCancelFailedDestination);
        TestUtils.assertQueuesEmptyThenClear(destinations, output);
    }

    protected abstract OutputDestination getOutputDestination();

    /**
     * Provide a computation manager for the worker service so that computation can run.
     */
    protected void initDynamicMarginCalculationWorkerServiceSpy() {
        Mockito.when(dynamicMarginCalculationWorkerService.getComputationManager()).thenReturn(computationManager);
    }

    /**
     * Concrete test classes must set up repository responses for parameters UUIDs they use.
     */
    protected abstract void initParametersRepositoryMock();

    /**
     * Concrete test classes may stub directory/filter calls if their parameters require it.
     */
    protected abstract void initExternalClientsMocks();

    // --- utility methods --- //

    protected void assertResultStatus(UUID runUuid, DynamicMarginCalculationStatus expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/results/{resultUuid}/status", runUuid))
                .andExpect(status().isOk())
                .andReturn();

        DynamicMarginCalculationStatus statusValue = null;
        if (!result.getResponse().getContentAsString().isEmpty()) {
            statusValue = objectMapper.readValue(result.getResponse().getContentAsString(), DynamicMarginCalculationStatus.class);
        }

        assertThat(statusValue).isSameAs(expectedStatus);
    }
}
