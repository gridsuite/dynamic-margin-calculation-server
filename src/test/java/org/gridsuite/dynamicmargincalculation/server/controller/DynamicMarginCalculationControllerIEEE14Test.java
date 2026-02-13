/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicmargincalculation.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.datasource.ResourceDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.contingency.Contingency;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.iidm.network.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.PreloadingStrategy;
import org.gridsuite.dynamicmargincalculation.server.controller.utils.DynamicModelConfigJsonUtils;
import org.gridsuite.dynamicmargincalculation.server.dto.DynamicMarginCalculationStatus;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.*;
import org.gridsuite.dynamicmargincalculation.server.entities.parameters.DynamicMarginCalculationParametersEntity;
import org.gridsuite.filter.expertfilter.ExpertFilter;
import org.gridsuite.filter.expertfilter.expertrule.CombinatorExpertRule;
import org.gridsuite.filter.expertfilter.expertrule.StringExpertRule;
import org.gridsuite.filter.utils.EquipmentType;
import org.gridsuite.filter.utils.expertfilter.CombinatorType;
import org.gridsuite.filter.utils.expertfilter.FieldType;
import org.gridsuite.filter.utils.expertfilter.OperatorType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.messaging.Message;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gridsuite.computation.service.AbstractResultContext.VARIANT_ID_HEADER;
import static org.gridsuite.computation.service.NotificationService.HEADER_RESULT_UUID;
import static org.gridsuite.computation.service.NotificationService.HEADER_USER_ID;
import static org.gridsuite.dynamicmargincalculation.server.controller.utils.TestUtils.RESOURCE_PATH_DELIMITER;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IEEE14 controller test for DynamicMarginCalculationController
 *
 * This test focuses on:
 * - calling the /v1/networks/{uuid}/run endpoint
 * - asserting a result notification is emitted
 * - asserting the persisted status ends as SUCCEED
 *
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicMarginCalculationControllerIEEE14Test extends AbstractDynamicMarginCalculationControllerTest {

    // directories
    public static final String DATA_IEEE14_BASE_DIR = RESOURCE_PATH_DELIMITER + "data" + RESOURCE_PATH_DELIMITER + "ieee14";
    public static final String INPUT = "input";

    public static final String DYNAMIC_MODEL_DUMP_FILE = "dynamicModel.dmp";
    public static final String DYNAMIC_SIMULATION_PARAMETERS_DUMP_FILE = "dynamicSimulationParameters.dmp";

    public static final String NETWORK_FILE = "IEEE14.iidm";
    private static final UUID NETWORK_UUID = UUID.randomUUID();
    private static final String VARIANT_1_ID = "variant_1";

    private static final UUID DSA_PARAMETERS_UUID = UUID.randomUUID();
    private static final UUID PARAMETERS_UUID = UUID.randomUUID();
    private static final UUID FILTER_UUID = UUID.randomUUID();

    @Autowired
    private OutputDestination output;

    @Override
    public OutputDestination getOutputDestination() {
        return output;
    }

    @Override
    protected void initParametersRepositoryMock() {
        // Use defaults from service to keep test stable across parameter schema changes
        DynamicMarginCalculationParametersInfos params = parametersService.getDefaultParametersValues("Dynawo");
        params.setLoadsVariations(List.of(
                LoadsVariationInfos.builder()
                        .loadFilters(List.of(IdNameInfos.builder().id(FILTER_UUID).build()))
                        .variation(10.0)
                        .active(true)
                        .build()
        ));

        DynamicMarginCalculationParametersEntity entity = new DynamicMarginCalculationParametersEntity(params);
        given(dynamicMarginCalculationParametersRepository.findById(PARAMETERS_UUID)).willReturn(Optional.of(entity));
    }

    @Override
    protected void initExternalClientsMocks() {
        // Mock for network
        ReadOnlyDataSource dataSource = new ResourceDataSource("IEEE14",
                new ResourceSet(DATA_IEEE14_BASE_DIR, NETWORK_FILE));
        Network network = Importers.importData("XIIDM", dataSource, null);
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_1_ID);
        given(networkStoreClient.getNetwork(NETWORK_UUID, PreloadingStrategy.COLLECTION)).willReturn(network);

        // Mock for the dynamic simulation client
        initDynamicSimulationClientsMock();

        // Mock for the dynamic security analysis client
        initDynamicSecurityAnalysisClientMock();

        // Mock for the filer client
        when(filterClient.getFilters(eq(List.of(FILTER_UUID))))
            .thenReturn(
                List.of(
                    ExpertFilter.builder()
                        .id(FILTER_UUID)
                        .rules(CombinatorExpertRule.builder()
                            .combinator(CombinatorType.AND)
                            .rules(List.of(
                                StringExpertRule.builder()
                                    .field(FieldType.ID)
                                    .operator(OperatorType.IS)
                                    .value("_LOAD__11_EC")
                                    .build()
                            ))
                            .build())
                        .equipmentType(EquipmentType.LOAD)
                    .build())
            );

    }

    private void initDynamicSimulationClientsMock() {

        try {
            String inputDir = DATA_IEEE14_BASE_DIR + RESOURCE_PATH_DELIMITER + INPUT;

            // load dynamicModel.dmp
            String dynamicModelFilePath = inputDir + RESOURCE_PATH_DELIMITER + DYNAMIC_MODEL_DUMP_FILE;
            InputStream dynamicModelIS = getClass().getResourceAsStream(dynamicModelFilePath);
            assert dynamicModelIS != null;
            // temporal :  use custom ObjectMapper provided by powsybl-dynawo to deserialize dynamic model
            ObjectMapper dynamicModelConfigObjectMapper = DynamicModelConfigJsonUtils.createObjectMapper();
            List<DynamicModelConfig> dynamicModel = dynamicModelConfigObjectMapper.readValue(dynamicModelIS, new TypeReference<>() { });

            // load dynamicSimulationParameters.dmp
            String dynamicSimulationParametersFilePath = inputDir + RESOURCE_PATH_DELIMITER + DYNAMIC_SIMULATION_PARAMETERS_DUMP_FILE;
            InputStream dynamicSimulationParametersIS = getClass().getResourceAsStream(dynamicSimulationParametersFilePath);
            assert dynamicSimulationParametersIS != null;
            DynamicSimulationParameters dynamicSimulationParameters = objectMapper.readValue(dynamicSimulationParametersIS, DynamicSimulationParameters.class);

            // Mock for dynamic simulation server
            when(dynamicSimulationClient.getParametersValues(anyString(), eq(NETWORK_UUID), any()))
                    .thenReturn(DynamicSimulationParametersValues.builder()
                            .dynamicModel(dynamicModel)
                            .dynawoParameters(dynamicSimulationParameters.getExtension(DynawoSimulationParameters.class))
                            .build());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void initDynamicSecurityAnalysisClientMock() {
        when(dynamicSecurityAnalysisClient.getParametersValues(eq(DSA_PARAMETERS_UUID), eq(NETWORK_UUID), any()))
                .thenReturn(DynamicSecurityAnalysisParametersValues.builder()
                        .contingenciesStartTime(105d)
                        .contingencies(List.of(Contingency.load("_LOAD__11_EC")))
                        .build());
    }

    @Disabled("To ignore test with container")
    @Test
    void test01IEEE14() throws Exception {
        // The controller requires a request body string: dynamicSimulationParametersJson.
        String dynamicSimulationParametersJson = "{}";

        // run dynamic margin calculation on a specific variant
        MvcResult result = mockMvc.perform(
                        post("/v1/networks/{networkUuid}/run", NETWORK_UUID.toString())
                                .param(VARIANT_ID_HEADER, VARIANT_1_ID)
                                .param("dynamicSecurityAnalysisParametersUuid", DSA_PARAMETERS_UUID.toString())
                                .param("parametersUuid", PARAMETERS_UUID.toString())
                                .contentType(APPLICATION_JSON)
                                .content(dynamicSimulationParametersJson)
                                .header(HEADER_USER_ID, "testUserId")
                )
                .andExpect(status().isOk())
                .andReturn();

        UUID runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        Message<byte[]> messageSwitch = output.receive(2000, dmcResultDestination);
        assertThat(messageSwitch).isNotNull();
        assertThat(messageSwitch.getHeaders()).containsEntry(HEADER_RESULT_UUID, runUuid.toString());

        // --- CHECK result --- //
        assertResultStatus(runUuid, DynamicMarginCalculationStatus.SUCCEED);
    }
}
