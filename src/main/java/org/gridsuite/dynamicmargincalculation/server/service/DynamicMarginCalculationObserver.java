/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.service;

import com.powsybl.dynawo.contingency.results.Status;
import com.powsybl.dynawo.margincalculation.results.MarginCalculationResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import lombok.NonNull;
import org.gridsuite.computation.service.AbstractComputationObserver;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.DynamicMarginCalculationParametersInfos;
import org.springframework.stereotype.Service;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class DynamicMarginCalculationObserver extends AbstractComputationObserver<MarginCalculationResult, DynamicMarginCalculationParametersInfos> {

    private static final String COMPUTATION_TYPE = "dynamicmargincalculation";

    public DynamicMarginCalculationObserver(@NonNull ObservationRegistry observationRegistry, @NonNull MeterRegistry meterRegistry) {
        super(observationRegistry, meterRegistry);
    }

    @Override
    protected String getComputationType() {
        return COMPUTATION_TYPE;
    }

    @Override
    protected String getResultStatus(MarginCalculationResult res) {
        return res != null && res.getLoadIncreaseResults().stream()
           .noneMatch(loadIncreaseResult -> loadIncreaseResult.status() == Status.EXECUTION_PROBLEM) ? "OK" : "NOK";
    }
}
