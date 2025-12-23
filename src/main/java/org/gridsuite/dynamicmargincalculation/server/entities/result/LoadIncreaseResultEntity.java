/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.entities.result;

import com.powsybl.dynawo.contingency.results.FailedCriterion;
import com.powsybl.dynawo.contingency.results.ScenarioResult;
import com.powsybl.dynawo.contingency.results.Status;
import com.powsybl.dynawo.margincalculation.results.LoadIncreaseResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "load_increase_result", indexes = {@Index(name = "load_increase_result_idx", columnList = "dynamic_margin_calculation_result_uuid")})
public class LoadIncreaseResultEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "load_level")
    double loadLevel;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    Status status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(
                name = "load_increase_result_id",
                referencedColumnName = "id",
                foreignKey = @ForeignKey(name = "scenario_result_load_increase_result_id_fk")
            )
    @OrderColumn(name = "pos")
    private List<ScenarioResultEntity> scenarioResults = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "load_increase_result_failed_criteria",
            joinColumns = @JoinColumn(
                name = "load_increase_result_id",
                referencedColumnName = "id",
                foreignKey = @ForeignKey(name = "load_increase_result_failed_criteria_load_increase_result_id_fk")
            ),
            indexes = {
                @Index(name = "load_increase_result_failed_criteria_idx", columnList = "load_increase_result_id")
            }
    )
    @OrderColumn(name = "pos")
    private List<FailedCriterionEmbeddable> failedCriteria = new ArrayList<>();

    public static LoadIncreaseResultEntity fromDomain(LoadIncreaseResult loadIncreaseResult) {
        LoadIncreaseResultEntity entity = new LoadIncreaseResultEntity();
        entity.setId(UUID.randomUUID());
        entity.setLoadLevel(loadIncreaseResult.loadLevel());
        entity.setStatus(loadIncreaseResult.status());

        List<ScenarioResult> scenarioResults = loadIncreaseResult.scenarioResults();
        entity.setScenarioResults(scenarioResults.stream().map(ScenarioResultEntity::fromDomain).toList());

        List<FailedCriterion> failedCriteria = loadIncreaseResult.failedCriteria();
        entity.setFailedCriteria(failedCriteria.stream().map(FailedCriterionEmbeddable::fromDomain).toList());

        return entity;
    }

    public LoadIncreaseResult toDto() {
        List<ScenarioResult> scenarioResultList = scenarioResults.stream().map(ScenarioResultEntity::toDto).toList();
        List<FailedCriterion> failedCriterionList = failedCriteria.stream().map(FailedCriterionEmbeddable::toDto).toList();

        return new LoadIncreaseResult(loadLevel, status, scenarioResultList, failedCriterionList);
    }
}

