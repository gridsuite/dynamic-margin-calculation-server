/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.entities.result;

import com.powsybl.dynawo.margincalculation.results.MarginCalculationResult;
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
@Getter
@Setter
@Table(name = "dynamic_margin_calculation_result")
@NoArgsConstructor
@Entity
public class MarginCalculationResultEntity {
    @Id
    @Column(name = "result_uuid")
    private UUID resultUuid;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(
            name = "dynamic_margin_calculation_result_uuid",
            referencedColumnName = "result_uuid",
            foreignKey = @ForeignKey(name = "load_increase_result_dynamic_margin_calculation_result_uuid_fk"))
    @OrderColumn(name = "pos")
    private List<LoadIncreaseResultEntity> loadIncreaseResults = new ArrayList<>();

    public static MarginCalculationResultEntity fromDomain(UUID resultUuid, MarginCalculationResult marginCalculationResult) {
        MarginCalculationResultEntity entity = new MarginCalculationResultEntity();
        entity.setResultUuid(resultUuid);

        entity.setLoadIncreaseResults(marginCalculationResult.getLoadIncreaseResults().stream().map(LoadIncreaseResultEntity::fromDomain).toList());

        return entity;
    }

    public MarginCalculationResult toDto() {
        return new MarginCalculationResult(loadIncreaseResults.stream().map(LoadIncreaseResultEntity::toDto).toList());
    }

}
