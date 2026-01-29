/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.dynamicmargincalculation.server.dto.DynamicMarginCalculationStatus;

import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Getter
@Setter
@Table(name = "dynamic_margin_calculation_status")
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class DynamicMarginCalculationStatusEntity {

    public DynamicMarginCalculationStatusEntity(UUID resultUuid, DynamicMarginCalculationStatus status) {
        this.resultUuid = resultUuid;
        this.status = status;
    }

    @Id
    @Column(name = "result_uuid")
    private UUID resultUuid;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DynamicMarginCalculationStatus status;

    @Column(name = "debugFileLocation")
    private String debugFileLocation;

}
