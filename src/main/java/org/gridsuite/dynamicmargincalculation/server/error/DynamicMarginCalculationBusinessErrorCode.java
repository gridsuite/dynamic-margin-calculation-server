/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicmargincalculation.server.error;

import com.powsybl.ws.commons.error.BusinessErrorCode;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public enum DynamicMarginCalculationBusinessErrorCode implements BusinessErrorCode {
    PROVIDER_NOT_FOUND("dynamicMarginCalculation.providerNotFound"),
    CONTINGENCIES_NOT_FOUND("dynamicMarginCalculation.contingenciesNotFound"),
    CONTINGENCY_LIST_EMPTY("dynamicMarginCalculation.contingencyListEmpty");

    private final String code;

    DynamicMarginCalculationBusinessErrorCode(String code) {
        this.code = code;
    }

    public String value() {
        return code;
    }
}
