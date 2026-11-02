/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicmargincalculation.server.dto.parameters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfigsJsonDeserializer;

import java.io.IOException;
import java.util.List;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicModelConfigListJsonDeserializer extends JsonDeserializer<List<DynamicModelConfig>> {

    @Override
    public List<DynamicModelConfig> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return new DynamicModelConfigsJsonDeserializer().deserialize(parser, context).dynamicModelConfigList();
    }
}
