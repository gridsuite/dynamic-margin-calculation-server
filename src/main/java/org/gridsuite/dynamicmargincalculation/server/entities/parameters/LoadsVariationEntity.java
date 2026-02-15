/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.entities.parameters;

import jakarta.persistence.*;
import lombok.*;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.IdNameInfos;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.LoadsVariationInfos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "loads_variation", indexes = @Index(name = "idx_loads_variation_dynamic_margin_calculation_parameters_id",
        columnList = "dynamic_margin_calculation_parameters_id"))
public class LoadsVariationEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "loads_variation_load_filter",
            joinColumns = @JoinColumn(name = "loads_variation_id"),
            foreignKey = @ForeignKey(name = "loads_variation_id_fk"),
            indexes = { @Index(name = "idx_loads_variation_load_filter_loads_variation_id", columnList = "loads_variation_id") }
    )
    @Column(name = "load_filter_id", nullable = false)
    private List<UUID> loadFilterIds = new ArrayList<>();

    private Double variation;

    private Boolean active; // means this variation is used in calculation

    LoadsVariationEntity(LoadsVariationInfos loadsVariationInfos) {
        assignAttributes(loadsVariationInfos);
    }

    public void assignAttributes(LoadsVariationInfos loadsVariationInfos) {
        if (id == null) {
            id = UUID.randomUUID();
        }
        variation = loadsVariationInfos.getVariation();
        active = loadsVariationInfos.getActive();
        loadFilterIds = loadsVariationInfos.getLoadFilters().stream().map(IdNameInfos::getId).toList();
    }

    void update(LoadsVariationInfos loadsVariationInfos) {
        assignAttributes(loadsVariationInfos);
    }

    LoadsVariationInfos toDto(boolean toDuplicate) {
        return LoadsVariationInfos.builder()
                .id(toDuplicate ? null : id)
                .variation(variation)
                .active(active)
                .loadFilters(loadFilterIds.stream().map(loadFilterId -> new IdNameInfos(loadFilterId, null)).toList())
                .build();
    }
}
