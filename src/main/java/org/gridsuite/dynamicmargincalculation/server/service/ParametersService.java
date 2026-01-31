/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicmargincalculation.server.service;

import com.powsybl.dynawo.margincalculation.MarginCalculation;
import com.powsybl.dynawo.margincalculation.MarginCalculationParameters;
import com.powsybl.dynawo.margincalculation.loadsvariation.LoadsVariation;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import jakarta.transaction.Transactional;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.computation.dto.ReportInfos;
import org.gridsuite.computation.error.ComputationException;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.DynamicMarginCalculationParametersInfos;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.LoadsVariationInfos;
import org.gridsuite.dynamicmargincalculation.server.entities.parameters.DynamicMarginCalculationParametersEntity;
import org.gridsuite.dynamicmargincalculation.server.error.DynamicMarginCalculationException;
import org.gridsuite.dynamicmargincalculation.server.repositories.DynamicMarginCalculationParametersRepository;
import org.gridsuite.dynamicmargincalculation.server.service.client.FilterClient;
import org.gridsuite.dynamicmargincalculation.server.service.contexts.DynamicMarginCalculationRunContext;
import org.gridsuite.filter.expertfilter.ExpertFilter;
import org.gridsuite.filter.expertfilter.expertrule.FilterUuidExpertRule;
import org.gridsuite.filter.utils.EquipmentType;
import org.gridsuite.filter.utils.FiltersUtils;
import org.gridsuite.filter.utils.expertfilter.FieldType;
import org.gridsuite.filter.utils.expertfilter.OperatorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.gridsuite.computation.error.ComputationBusinessErrorCode.PARAMETERS_NOT_FOUND;
import static org.gridsuite.dynamicmargincalculation.server.error.DynamicMarginCalculationBusinessErrorCode.PROVIDER_NOT_FOUND;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class ParametersService {

    public static final String MSG_PARAMETERS_UUID_NOT_FOUND = "Parameters uuid not found: ";

    private final String defaultProvider;

    private final DynamicMarginCalculationParametersRepository dynamicMarginCalculationParametersRepository;

    private final FilterClient filterClient;

    @Autowired
    public ParametersService(@Value("${dynamic-margin-calculation.default-provider}") String defaultProvider,
                             DynamicMarginCalculationParametersRepository dynamicMarginCalculationParametersRepository,
                             FilterClient filterClient) {
        this.defaultProvider = defaultProvider;
        this.dynamicMarginCalculationParametersRepository = dynamicMarginCalculationParametersRepository;
        this.filterClient = filterClient;
    }

    public DynamicMarginCalculationRunContext createRunContext(UUID networkUuid, String variantId, String receiver,
           String provider, ReportInfos reportInfos, String userId,
           // should be UUID dynamicSimulationParametersUuid after moving dynamic simulation parameters to its server,
           String dynamicSimulationParametersJson,
           UUID dynamicSecurityAnalysisParametersUuid,
           UUID dynamicMarginCalculationParametersUuid,
           boolean debug) {

        // get parameters from the local database
        DynamicMarginCalculationParametersInfos dynamicMarginCalculationParametersInfos = getParameters(dynamicMarginCalculationParametersUuid);
        // take only active load variations
        dynamicMarginCalculationParametersInfos.setLoadsVariations(dynamicMarginCalculationParametersInfos.getLoadsVariations()
            .stream()
            .filter(LoadsVariationInfos::getActive).toList());

        // build run context
        DynamicMarginCalculationRunContext runContext = DynamicMarginCalculationRunContext.builder()
                .networkUuid(networkUuid)
                .variantId(variantId)
                .receiver(receiver)
                .reportInfos(reportInfos)
                .userId(userId)
                .parameters(dynamicMarginCalculationParametersInfos)
                .debug(debug)
                .build();
        runContext.setDynamicSimulationParametersJson(dynamicSimulationParametersJson);
        runContext.setDynamicSecurityAnalysisParametersUuid(dynamicSecurityAnalysisParametersUuid);

        // set provider for run context
        String providerToUse = provider;
        if (providerToUse == null) {
            providerToUse = Optional.ofNullable(runContext.getParameters().getProvider()).orElse(defaultProvider);
        }

        runContext.setProvider(providerToUse);

        // check provider
        if (!MarginCalculation.getRunner().getName().equals(runContext.getProvider())) {
            throw new DynamicMarginCalculationException(PROVIDER_NOT_FOUND, "Dynamic margin calculation provider not found: " + runContext.getProvider());
        }

        return runContext;
    }

    // --- Dynamic security analysis parameters related methods --- //

    public DynamicMarginCalculationParametersInfos getParameters(UUID parametersUuid) {
        DynamicMarginCalculationParametersEntity entity = dynamicMarginCalculationParametersRepository.findById(parametersUuid)
                .orElseThrow(() -> new ComputationException(PARAMETERS_NOT_FOUND, MSG_PARAMETERS_UUID_NOT_FOUND + parametersUuid));

        return entity.toDto(false);
    }

    public UUID createParameters(DynamicMarginCalculationParametersInfos parametersInfos) {
        return dynamicMarginCalculationParametersRepository.save(new DynamicMarginCalculationParametersEntity(parametersInfos)).getId();
    }

    @Transactional
    public UUID createDefaultParameters() {
        DynamicMarginCalculationParametersInfos defaultParametersInfos = getDefaultParametersValues(defaultProvider);
        return createParameters(defaultParametersInfos);
    }

    public DynamicMarginCalculationParametersInfos getDefaultParametersValues(String provider) {
        MarginCalculationParameters defaultConfigParameters = MarginCalculationParameters.load();
        return DynamicMarginCalculationParametersInfos.builder()
                .provider(provider)
                .startTime(defaultConfigParameters.getStartTime())
                .stopTime(defaultConfigParameters.getStopTime())
                .marginCalculationStartTime(defaultConfigParameters.getMarginCalculationStartTime())
                .loadIncreaseStartTime(defaultConfigParameters.getLoadIncreaseStartTime())
                .loadIncreaseStopTime(defaultConfigParameters.getLoadIncreaseStopTime())
                .calculationType(defaultConfigParameters.getCalculationType())
                .accuracy(defaultConfigParameters.getAccuracy())
                .loadModelsRule(defaultConfigParameters.getLoadModelsRule())
                .build();
    }

    @Transactional
    public UUID duplicateParameters(UUID sourceParametersUuid) {
        DynamicMarginCalculationParametersEntity entity = dynamicMarginCalculationParametersRepository.findById(sourceParametersUuid)
                .orElseThrow(() -> new ComputationException(PARAMETERS_NOT_FOUND, MSG_PARAMETERS_UUID_NOT_FOUND + sourceParametersUuid));
        DynamicMarginCalculationParametersInfos duplicatedParametersInfos = entity.toDto(true);
        duplicatedParametersInfos.setId(null);
        return createParameters(duplicatedParametersInfos);
    }

    public List<DynamicMarginCalculationParametersInfos> getAllParameters() {
        return dynamicMarginCalculationParametersRepository.findAll().stream()
                .map(paramsEntity -> paramsEntity.toDto(false))
                .toList();
    }

    @Transactional
    public void updateParameters(UUID parametersUuid, DynamicMarginCalculationParametersInfos parametersInfos) {
        DynamicMarginCalculationParametersEntity entity = dynamicMarginCalculationParametersRepository.findById(parametersUuid)
                .orElseThrow(() -> new ComputationException(PARAMETERS_NOT_FOUND, MSG_PARAMETERS_UUID_NOT_FOUND + parametersUuid));
        if (parametersInfos == null) {
            // if the parameter is null, it means it's a reset to defaultValues, but we need to keep the provider because it's updated separately
            entity.update(getDefaultParametersValues(Optional.ofNullable(entity.getProvider()).orElse(defaultProvider)));
        } else {
            entity.update(parametersInfos);
        }
    }

    public void deleteParameters(UUID parametersUuid) {
        dynamicMarginCalculationParametersRepository.deleteById(parametersUuid);
    }

    @Transactional
    public void updateProvider(UUID parametersUuid, String provider) {
        DynamicMarginCalculationParametersEntity entity = dynamicMarginCalculationParametersRepository.findById(parametersUuid)
                .orElseThrow(() -> new ComputationException(PARAMETERS_NOT_FOUND, MSG_PARAMETERS_UUID_NOT_FOUND + parametersUuid));
        entity.setProvider(provider != null ? provider : defaultProvider);
    }

    public List<LoadsVariation> getLoadsVariations(List<LoadsVariationInfos> loadsVariationInfosList, Network network) {
        if (CollectionUtils.isEmpty(loadsVariationInfosList)) {
            return Collections.emptyList();
        }

        List<LoadsVariation> loadsVariations = loadsVariationInfosList.stream().map(loadsVariationInfos -> {
            // build as a unique IS_PART_OF expert-filter then evaluate
            ExpertFilter filter = ExpertFilter.builder()
                    .equipmentType(EquipmentType.LOAD)
                    .rules(FilterUuidExpertRule.builder()
                            .field(FieldType.ID)
                            .operator(OperatorType.IS_PART_OF)
                            .values(loadsVariationInfos.getLoadFilterUuids().stream().map(UUID::toString).collect(Collectors.toSet()))
                            .build())
                    .build();

            List<Load> loads = FiltersUtils.getIdentifiables(filter, network, filterClient::getFilters).stream()
                    .map(Load.class::cast).toList();
            return new LoadsVariation(loads, loadsVariationInfos.getVariation());
        }).toList();

        return loadsVariations;
    }

}
