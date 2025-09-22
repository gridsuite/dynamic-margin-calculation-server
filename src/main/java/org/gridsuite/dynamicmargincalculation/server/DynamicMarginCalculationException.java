/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicmargincalculation.server;

import lombok.Getter;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Getter
public class DynamicMarginCalculationException extends RuntimeException {

    public enum Type {
        RESULT_UUID_NOT_FOUND,
    }

    private final Type type;

    public DynamicMarginCalculationException(Type type, String message) {
        super(message);
        this.type = type;
    }
}
