/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.entities.parameters;

import com.powsybl.dynawo.margincalculation.MarginCalculationParameters.CalculationType;
import com.powsybl.dynawo.margincalculation.MarginCalculationParameters.LoadModelsRule;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.DynamicMarginCalculationParametersInfos;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.LoadsVariationInfos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static jakarta.persistence.CascadeType.ALL;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "dynamic_margin_calculation_parameters")
public class DynamicMarginCalculationParametersEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "provider")
    private String provider;

    @Column(name = "start_time")
    private Double startTime;

    @Column(name = "stop_time")
    private Double stopTime;

    @Column(name = "margin_calculation_start_time")
    private Double marginCalculationStartTime;

    @Column(name = "load_increase_start_time")
    private Double loadIncreaseStartTime;

    @Column(name = "load_increase_stop_time")
    private Double loadIncreaseStopTime;

    @Column(name = "calculation_type")
    @Enumerated(EnumType.STRING)
    private CalculationType calculationType;

    @Column(name = "accuracy")
    private Integer accuracy;

    @Column(name = "load_models_rule")
    @Enumerated(EnumType.STRING)
    private LoadModelsRule loadModelsRule;

    @OneToMany(fetch = FetchType.EAGER, cascade = ALL, orphanRemoval = true)
    @JoinColumn(name = "dynamic_margin_calculation_parameters_id", foreignKey = @ForeignKey(name = "dynamic_margin_calculation_parameters_id_fk"))
    @OrderColumn(name = "pos")
    private List<LoadsVariationEntity> loadsVariations = new ArrayList<>();

    public DynamicMarginCalculationParametersEntity(DynamicMarginCalculationParametersInfos parametersInfos) {
        assignAttributes(parametersInfos);
    }

    private void assignAttributes(DynamicMarginCalculationParametersInfos parametersInfos) {
        if (id == null) {
            id = UUID.randomUUID();
            parametersInfos.setId(id);
        }
        provider = parametersInfos.getProvider();
        startTime = parametersInfos.getStartTime();
        stopTime = parametersInfos.getStopTime();
        marginCalculationStartTime = parametersInfos.getMarginCalculationStartTime();
        loadIncreaseStartTime = parametersInfos.getLoadIncreaseStartTime();
        loadIncreaseStopTime = parametersInfos.getLoadIncreaseStopTime();
        calculationType = parametersInfos.getCalculationType();
        accuracy = parametersInfos.getAccuracy();
        loadModelsRule = parametersInfos.getLoadModelsRule();
        // assign loads variations
        assignLoadsVariations(parametersInfos.getLoadsVariations());
    }

    private void assignLoadsVariations(List<LoadsVariationInfos> loadsVariationInfosList) {
        if (CollectionUtils.isEmpty(loadsVariationInfosList)) {
            return;
        }
        // build existing loads variation Map
        Map<UUID, LoadsVariationEntity> loadsVariationsByIdMap = loadsVariations.stream().collect(Collectors.toMap(LoadsVariationEntity::getId, loadVariationEntity -> loadVariationEntity));

        // merge existing and add new loads variations
        List<LoadsVariationEntity> mergedLoadsVariations = new ArrayList<>();
        for (LoadsVariationInfos loadsVariationInfos : loadsVariationInfosList) {
            if (loadsVariationInfos.getId() != null) {
                LoadsVariationEntity existingEntity = loadsVariationsByIdMap.get(loadsVariationInfos.getId());
                existingEntity.update(loadsVariationInfos);
                mergedLoadsVariations.add(existingEntity);
            } else {
                mergedLoadsVariations.add(new LoadsVariationEntity(loadsVariationInfos));
            }
        }

        // by clear/addAll, existing elements that are not present in the new list will be removed systematically
        loadsVariations.clear();
        loadsVariations.addAll(mergedLoadsVariations);
    }

    public void update(DynamicMarginCalculationParametersInfos parametersInfos) {
        assignAttributes(parametersInfos);
    }

    public DynamicMarginCalculationParametersInfos toDto(boolean toDuplicate) {
        return DynamicMarginCalculationParametersInfos.builder()
            .id(toDuplicate ? null : id)
            .provider(provider)
            .startTime(startTime)
            .stopTime(stopTime)
            .marginCalculationStartTime(marginCalculationStartTime)
            .loadIncreaseStartTime(loadIncreaseStartTime)
            .loadIncreaseStopTime(loadIncreaseStopTime)
            .calculationType(calculationType)
            .accuracy(accuracy)
            .loadModelsRule(loadModelsRule)
            .loadsVariations(loadsVariations.stream().map(loadsVariationEntity -> loadsVariationEntity.toDto(toDuplicate)).toList())
            .build();
    }
}
