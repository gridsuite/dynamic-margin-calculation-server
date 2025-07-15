/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.service;

import org.gridsuite.dynamicmargincalculation.server.repositories.DynamicMarginCalculationResultRepository;
import org.springframework.stereotype.Service;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class SupervisionService {
    private final DynamicMarginCalculationResultRepository resultRepository;

    public SupervisionService(DynamicMarginCalculationResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    public Integer getResultsCount() {
        return (int) resultRepository.count();
    }
}
