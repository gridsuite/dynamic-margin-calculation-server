/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicmargincalculation.server.service.contexts;

import com.powsybl.contingency.Contingency;
import com.powsybl.dynawo.margincalculation.MarginCalculationParameters;
import com.powsybl.dynawo.margincalculation.loadsvariation.LoadsVariation;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.gridsuite.computation.dto.ReportInfos;
import org.gridsuite.computation.service.AbstractComputationRunContext;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.DynamicMarginCalculationParametersInfos;

import java.util.List;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Getter
@Setter
public class DynamicMarginCalculationRunContext extends AbstractComputationRunContext<DynamicMarginCalculationParametersInfos> {

    private String dynamicSimulationParametersJson;
    private UUID dynamicSecurityAnalysisParametersUuid;

    // --- Fields which are enriched in worker service --- //

    private List<DynamicModelConfig> dynamicModel;
    private MarginCalculationParameters marginCalculationParameters;
    private List<Contingency> contingencies;
    private List<LoadsVariation> loadsVariations;

    @Builder
    public DynamicMarginCalculationRunContext(UUID networkUuid, String variantId, String receiver, String provider,
                                              ReportInfos reportInfos, String userId, DynamicMarginCalculationParametersInfos parameters, Boolean debug) {
        super(networkUuid, variantId, receiver, reportInfos, userId, provider, parameters, debug);
    }
}

