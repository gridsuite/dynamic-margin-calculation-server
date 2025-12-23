/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.entities.result;

import com.powsybl.dynawo.contingency.results.FailedCriterion;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
@Embeddable
public class FailedCriterionEmbeddable {

    @Column(name = "description")
    private String description;

    @Column(name = "time")
    private double time;

    public static FailedCriterionEmbeddable fromDomain(FailedCriterion failedCriterion) {
        FailedCriterionEmbeddable embeddable = new FailedCriterionEmbeddable();
        embeddable.setDescription(failedCriterion.description());
        embeddable.setTime(failedCriterion.time());
        return embeddable;
    }

    public FailedCriterion toDto() {
        return new FailedCriterion(description, time);
    }
}
