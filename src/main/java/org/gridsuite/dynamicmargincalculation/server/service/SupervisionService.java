/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.service;

import org.gridsuite.dynamicmargincalculation.server.repositories.DynamicMarginCalculationStatusRepository;
import org.springframework.stereotype.Service;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class SupervisionService {
    private final DynamicMarginCalculationStatusRepository statusRepository;

    public SupervisionService(DynamicMarginCalculationStatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    public Integer getResultsCount() {
        return (int) statusRepository.count();
    }
}
