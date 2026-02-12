/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicmargincalculation.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.dynamicmargincalculation.server.DynamicMarginCalculationApplication;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.DynamicMarginCalculationParametersInfos;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.IdNameInfos;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.LoadsVariationInfos;
import org.gridsuite.dynamicmargincalculation.server.entities.parameters.DynamicMarginCalculationParametersEntity;
import org.gridsuite.dynamicmargincalculation.server.repositories.DynamicMarginCalculationParametersRepository;
import org.gridsuite.dynamicmargincalculation.server.service.FilterService;
import org.gridsuite.dynamicmargincalculation.server.service.ParametersService;
import org.gridsuite.dynamicmargincalculation.server.service.client.DirectoryClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gridsuite.computation.service.NotificationService.HEADER_USER_ID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest
@ContextConfiguration(classes = {DynamicMarginCalculationApplication.class})
class DynamicMarginCalculationParametersControllerTest {

    private static final String USER_ID = "userId";
    private static final UUID LOAD_FILTER_UUID_1 = UUID.fromString("fff118fa-12ff-4fe1-965d-1d81a45d8ef8");
    private static final UUID LOAD_FILTER_UUID_2 = UUID.fromString("96d0097b-ec80-4ac9-827a-4a38095972a0");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ParametersService parametersService;

    @Autowired
    DynamicMarginCalculationParametersRepository parametersRepository;

    @MockitoBean
    DirectoryClient directoryClient;

    @MockitoBean
    FilterService filterService;

    @AfterEach
    void tearDown() {
        parametersRepository.deleteAll();
        reset(directoryClient, filterService);
    }

    private DynamicMarginCalculationParametersInfos newParametersInfos() {
        // Keep the DTO minimal and stable for CRUD tests (no need to involve filter evaluation).
        DynamicMarginCalculationParametersInfos infos = parametersService.getDefaultParametersValues("Dynawo");
        infos.setLoadsVariations(List.of()); // make explicit to avoid null handling differences
        return infos;
    }

    private DynamicMarginCalculationParametersInfos newParametersInfosWithLoadFilters() {
        DynamicMarginCalculationParametersInfos infos = parametersService.getDefaultParametersValues("Dynawo");

        LoadsVariationInfos loadsVariationInfos = LoadsVariationInfos.builder()
                .active(true)
                .variation(10.0)
                .loadFilters(List.of(
                        IdNameInfos.builder().id(LOAD_FILTER_UUID_1).build(),
                        IdNameInfos.builder().id(LOAD_FILTER_UUID_2).build()
                ))
                .build();

        infos.setLoadsVariations(List.of(loadsVariationInfos));
        return infos;
    }

    @Test
    void testCreateParameters() throws Exception {
        DynamicMarginCalculationParametersInfos parametersInfos = newParametersInfos();

        MvcResult result = mockMvc.perform(post("/v1/parameters")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parametersInfos)))
                .andExpect(status().isOk())
                .andReturn();

        UUID parametersUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        Optional<DynamicMarginCalculationParametersEntity> entityOpt = parametersRepository.findById(parametersUuid);
        assertThat(entityOpt).isPresent();

        DynamicMarginCalculationParametersInfos resultParametersInfos = entityOpt.get().toDto(true);

        assertThat(resultParametersInfos).usingRecursiveComparison().isEqualTo(parametersInfos);
    }

    @Test
    void testCreateDefaultParameters() throws Exception {
        DynamicMarginCalculationParametersInfos defaultParametersInfos = newParametersInfos();

        MvcResult result = mockMvc.perform(post("/v1/parameters/default"))
                .andExpect(status().isOk())
                .andReturn();

        UUID parametersUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        Optional<DynamicMarginCalculationParametersEntity> entityOpt = parametersRepository.findById(parametersUuid);
        assertThat(entityOpt).isPresent();
        assertThat(entityOpt.get().getProvider()).isEqualTo("Dynawo");
        DynamicMarginCalculationParametersInfos resultParametersInfos = entityOpt.get().toDto(true);
        assertThat(resultParametersInfos).usingRecursiveComparison().isEqualTo(defaultParametersInfos);
    }

    @Test
    void testDuplicateParameters() throws Exception {
        DynamicMarginCalculationParametersInfos originalInfos = newParametersInfos();
        UUID originalUuid = parametersRepository.save(new DynamicMarginCalculationParametersEntity(originalInfos)).getId();

        MvcResult result = mockMvc.perform(post("/v1/parameters")
                        .param("duplicateFrom", originalUuid.toString()))
                .andExpect(status().isOk())
                .andReturn();

        UUID duplicatedUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        Optional<DynamicMarginCalculationParametersEntity> duplicatedOpt = parametersRepository.findById(duplicatedUuid);
        assertThat(duplicatedOpt).isPresent();

        DynamicMarginCalculationParametersInfos duplicatedInfos = duplicatedOpt.get().toDto(true);

        assertThat(duplicatedInfos).usingRecursiveComparison().isEqualTo(originalInfos);
    }

    @Test
    void testGetParametersRequiresUserHeaderAndEnrichesLoadFilterNames() throws Exception {
        // --- Setup: persist parameters with load filters (names missing before enrichment) --- //
        DynamicMarginCalculationParametersInfos infos = newParametersInfosWithLoadFilters();
        UUID parametersUuid = parametersRepository.save(new DynamicMarginCalculationParametersEntity(infos)).getId();

        // service enrichment uses DirectoryClient only when userId is provided
        when(directoryClient.getElementNames(eq(List.of(LOAD_FILTER_UUID_1, LOAD_FILTER_UUID_2)), eq(USER_ID)))
                .thenReturn(Map.of(
                        LOAD_FILTER_UUID_1, "Filter 1",
                        LOAD_FILTER_UUID_2, "Filter 2"
                ));

        // --- Execute --- //
        MvcResult result = mockMvc.perform(get("/v1/parameters/{uuid}", parametersUuid)
                        .header(HEADER_USER_ID, USER_ID))
                .andExpect(status().isOk())
                .andReturn();

        DynamicMarginCalculationParametersInfos returned =
                objectMapper.readValue(result.getResponse().getContentAsString(), DynamicMarginCalculationParametersInfos.class);

        // --- Verify --- //
        assertThat(returned.getId()).isEqualTo(parametersUuid);
        assertThat(returned.getLoadsVariations()).hasSize(1);
        assertThat(returned.getLoadsVariations().getFirst().getLoadFilters()).hasSize(2);

        assertThat(returned.getLoadsVariations().getFirst().getLoadFilters().getFirst().getName()).isEqualTo("Filter 1");
        assertThat(returned.getLoadsVariations().getFirst().getLoadFilters().get(1).getName()).isEqualTo("Filter 2");

        verify(directoryClient, times(1)).getElementNames(eq(List.of(LOAD_FILTER_UUID_1, LOAD_FILTER_UUID_2)), eq(USER_ID));
    }

    @Test
    void testGetAllParameters() throws Exception {
        DynamicMarginCalculationParametersInfos infos = newParametersInfos();
        parametersRepository.saveAll(List.of(
                new DynamicMarginCalculationParametersEntity(infos),
                new DynamicMarginCalculationParametersEntity(infos)
        ));

        MvcResult result = mockMvc.perform(get("/v1/parameters"))
                .andExpect(status().isOk())
                .andReturn();

        List<DynamicMarginCalculationParametersInfos> returned = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() { }
        );

        assertThat(returned).hasSize(2);
        assertThat(returned.get(0).getId()).isNotNull();
        assertThat(returned.get(1).getId()).isNotNull();
    }

    @Test
    void testUpdateParameters() throws Exception {
        DynamicMarginCalculationParametersInfos infos = newParametersInfos();
        UUID parametersUuid = parametersRepository.save(new DynamicMarginCalculationParametersEntity(infos)).getId();

        DynamicMarginCalculationParametersInfos updatedInfos = newParametersInfos();
        updatedInfos.setAccuracy(1); // change a field to ensure update persists

        mockMvc.perform(put("/v1/parameters/{uuid}", parametersUuid)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedInfos)))
                .andExpect(status().isOk());

        Optional<DynamicMarginCalculationParametersEntity> entityOpt = parametersRepository.findById(parametersUuid);
        assertThat(entityOpt).isPresent();

        DynamicMarginCalculationParametersInfos persisted = entityOpt.get().toDto(false);
        assertThat(persisted.getAccuracy()).isEqualTo(1);
    }

    @Test
    void testDeleteParameters() throws Exception {
        DynamicMarginCalculationParametersInfos infos = newParametersInfos();
        UUID parametersUuid = parametersRepository.save(new DynamicMarginCalculationParametersEntity(infos)).getId();

        mockMvc.perform(delete("/v1/parameters/{uuid}", parametersUuid))
                .andExpect(status().isOk());

        assertThat(parametersRepository.findById(parametersUuid)).isEmpty();
    }

    @Test
    void testGetProvider() throws Exception {
        DynamicMarginCalculationParametersInfos infos = newParametersInfos();
        infos.setProvider("Dynawo");
        UUID parametersUuid = parametersRepository.save(new DynamicMarginCalculationParametersEntity(infos)).getId();

        MvcResult result = mockMvc.perform(get("/v1/parameters/{uuid}/provider", parametersUuid))
                .andExpect(status().isOk())
                .andReturn();
        String provider = result.getResponse().getContentAsString();
        assertThat(provider).isEqualTo("Dynawo");
    }

    @Test
    void testUpdateProvider() throws Exception {
        DynamicMarginCalculationParametersInfos infos = newParametersInfos();
        UUID parametersUuid = parametersRepository.save(new DynamicMarginCalculationParametersEntity(infos)).getId();

        String newProvider = "Dynawo";

        mockMvc.perform(put("/v1/parameters/{uuid}/provider", parametersUuid)
                        .contentType(TEXT_PLAIN_VALUE)
                        .content(newProvider))
                .andExpect(status().isOk());

        Optional<DynamicMarginCalculationParametersEntity> entityOpt = parametersRepository.findById(parametersUuid);
        assertThat(entityOpt).isPresent();
        assertThat(entityOpt.get().getProvider()).isEqualTo(newProvider);
    }
}
