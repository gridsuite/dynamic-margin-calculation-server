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
@Table(name = "scenario_result", indexes = @Index(name = "scenario_result_idx", columnList = "load_increase_result_id")
)
public class ScenarioResultEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "equipment_id")
    private String equipmentId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @ElementCollection
    @CollectionTable(
            name = "scenario_result_failed_criteria",
            joinColumns = @JoinColumn(
                name = "scenario_result_id",
                referencedColumnName = "id",
                foreignKey = @ForeignKey(name = "scenario_result_failed_criteria_scenario_result_id_fk")
            ),
            indexes = {
                @Index(name = "scenario_result_failed_criteria_idx", columnList = "scenario_result_id")
            }
    )
    @OrderColumn(name = "pos")
    private List<FailedCriterionEmbeddable> failedCriteria = new ArrayList<>();

    public static ScenarioResultEntity fromDomain(ScenarioResult scenarioResult) {
        ScenarioResultEntity embeddable = new ScenarioResultEntity();
        embeddable.setId(UUID.randomUUID());
        embeddable.setStatus(scenarioResult.status());

        List<FailedCriterion> failedCriteriaList = scenarioResult.failedCriteria();
        embeddable.setFailedCriteria(failedCriteriaList.stream().map(FailedCriterionEmbeddable::fromDomain).toList());

        return embeddable;
    }

    public ScenarioResult toDto() {
        List<FailedCriterion> failedCriterionList = failedCriteria.stream().map(FailedCriterionEmbeddable::toDto).toList();

        return new ScenarioResult(equipmentId, status, failedCriterionList);
    }
}
