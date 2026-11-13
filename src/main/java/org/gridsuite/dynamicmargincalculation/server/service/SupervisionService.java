/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.service;

import org.gridsuite.dynamicmargincalculation.server.repositories.DynamicMarginCalculationStatusRepository;
import org.gridsuite.dynamicmargincalculation.server.repositories.MarginCalculationResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class SupervisionService {
    private final DynamicMarginCalculationStatusRepository statusRepository;
    private final MarginCalculationResultRepository resultRepository;

    public SupervisionService(DynamicMarginCalculationStatusRepository statusRepository, MarginCalculationResultRepository resultRepository) {
        this.statusRepository = statusRepository;
        this.resultRepository = resultRepository;
    }

    public Integer getResultsCount() {
        return (int) statusRepository.count();
    }

    @Transactional
    public void deleteAllResults() {
        statusRepository.deleteAll();
        resultRepository.deleteAll();
    }
}
