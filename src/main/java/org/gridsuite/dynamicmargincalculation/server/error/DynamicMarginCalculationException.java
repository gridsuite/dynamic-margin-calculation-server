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

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Getter
public class DynamicMarginCalculationException extends AbstractBusinessException {

    private final DynamicMarginCalculationBusinessErrorCode errorCode;

    @NonNull
    @Override
    public DynamicMarginCalculationBusinessErrorCode getBusinessErrorCode() {
        return errorCode;
    }

    public DynamicMarginCalculationException(DynamicMarginCalculationBusinessErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
