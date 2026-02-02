/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicmargincalculation.server.error;

import com.powsybl.ws.commons.error.AbstractBusinessException;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;
import java.util.Objects;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Getter
public class DynamicMarginCalculationException extends AbstractBusinessException {

    private final DynamicMarginCalculationBusinessErrorCode errorCode;

    private final transient Map<String, Object> businessErrorValues;

    @NonNull
    @Override
    public DynamicMarginCalculationBusinessErrorCode getBusinessErrorCode() {
        return errorCode;
    }

    @NonNull
    @Override
    public Map<String, Object> getBusinessErrorValues() {
        return businessErrorValues;
    }

    public DynamicMarginCalculationException(DynamicMarginCalculationBusinessErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.businessErrorValues = Map.of();
    }

    public DynamicMarginCalculationException(DynamicMarginCalculationBusinessErrorCode errorCode, String message, Map<String, Object> businessErrorValues) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.businessErrorValues = businessErrorValues != null ? Map.copyOf(businessErrorValues) : Map.of();
    }

}
