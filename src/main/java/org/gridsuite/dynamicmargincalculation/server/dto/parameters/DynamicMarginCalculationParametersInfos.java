/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.dto.parameters;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.powsybl.dynawo.margincalculation.MarginCalculationParameters.CalculationType;
import com.powsybl.dynawo.margincalculation.MarginCalculationParameters.LoadModelsRule;
import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamicMarginCalculationParametersInfos {
    private UUID id;

    private String provider;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Double startTime;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Double stopTime;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Double marginCalculationStartTime;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Double loadIncreaseStartTime;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Double loadIncreaseStopTime;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private CalculationType calculationType;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Integer accuracy;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private LoadModelsRule loadModelsRule;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<LoadsVariationInfos> loadsVariations;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<UUID, String> elementsUuidToName;

}
