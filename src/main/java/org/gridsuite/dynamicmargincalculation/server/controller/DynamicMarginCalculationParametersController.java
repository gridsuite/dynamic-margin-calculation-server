/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicmargincalculation.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.dynamicmargincalculation.server.DynamicMarginCalculationApi;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.DynamicMarginCalculationParametersInfos;
import org.gridsuite.dynamicmargincalculation.server.service.ParametersService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.gridsuite.computation.service.NotificationService.HEADER_USER_ID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + DynamicMarginCalculationApi.API_VERSION + "/parameters")
@Tag(name = "Dynamic security analysis server - Parameters")
public class DynamicMarginCalculationParametersController {

    private final ParametersService parametersService;

    public DynamicMarginCalculationParametersController(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create parameters")
    @ApiResponse(responseCode = "200", description = "parameters were created")
    public ResponseEntity<UUID> createParameters(
            @RequestBody DynamicMarginCalculationParametersInfos parametersInfos) {
        return ResponseEntity.ok(parametersService.createParameters(parametersInfos));
    }

    @PostMapping(value = "/default")
    @Operation(summary = "Create default parameters")
    @ApiResponse(responseCode = "200", description = "Default parameters were created")
    public ResponseEntity<UUID> createDefaultParameters() {
        return ResponseEntity.ok(parametersService.createDefaultParameters());
    }

    @PostMapping(value = "", params = "duplicateFrom", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Duplicate parameters")
    @ApiResponse(responseCode = "200", description = "parameters were duplicated")
    public ResponseEntity<UUID> duplicateParameters(
            @Parameter(description = "source parameters UUID") @RequestParam("duplicateFrom") UUID sourceParametersUuid) {
        return ResponseEntity.ok(parametersService.duplicateParameters(sourceParametersUuid));
    }

    @GetMapping(value = "/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get parameters")
    @ApiResponse(responseCode = "200", description = "parameters were returned")
    @ApiResponse(responseCode = "404", description = "parameters were not found")
    public ResponseEntity<DynamicMarginCalculationParametersInfos> getParameters(
            @Parameter(description = "parameters UUID") @PathVariable("uuid") UUID parametersUuid,
            @RequestHeader(HEADER_USER_ID) String userId) {
        return ResponseEntity.ok(parametersService.getParameters(parametersUuid, userId));
    }

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all parameters")
    @ApiResponse(responseCode = "200", description = "The list of all parameters was returned")
    public ResponseEntity<List<DynamicMarginCalculationParametersInfos>> getAllParameters() {
        return ResponseEntity.ok().body(parametersService.getAllParameters());
    }

    @PutMapping(value = "/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update parameters")
    @ApiResponse(responseCode = "200", description = "parameters were updated")
    public ResponseEntity<Void> updateParameters(
            @Parameter(description = "parameters UUID") @PathVariable("uuid") UUID parametersUuid,
            @RequestBody(required = false) DynamicMarginCalculationParametersInfos parametersInfos) {
        parametersService.updateParameters(parametersUuid, parametersInfos);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{uuid}")
    @Operation(summary = "Delete parameters")
    @ApiResponse(responseCode = "200", description = "parameters were deleted")
    public ResponseEntity<Void> deleteParameters(
            @Parameter(description = "parameters UUID") @PathVariable("uuid") UUID parametersUuid) {
        parametersService.deleteParameters(parametersUuid);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{uuid}/provider", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get provider")
    @ApiResponse(responseCode = "200", description = "provider were returned")
    @ApiResponse(responseCode = "404", description = "provider were not found")
    public ResponseEntity<String> getProvider(
            @Parameter(description = "parameters UUID") @PathVariable("uuid") UUID parametersUuid) {
        return ResponseEntity.ok(parametersService.getProvider(parametersUuid));
    }

    @PutMapping(value = "/{uuid}/provider")
    @Operation(summary = "Update provider")
    @ApiResponse(responseCode = "200", description = "provider was updated")
    public ResponseEntity<Void> updateProvider(
            @Parameter(description = "parameters UUID") @PathVariable("uuid") UUID parametersUuid,
            @RequestBody(required = false) String provider) {
        parametersService.updateProvider(parametersUuid, provider);
        return ResponseEntity.ok().build();
    }

}
