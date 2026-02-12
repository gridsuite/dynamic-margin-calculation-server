/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicmargincalculation.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.computation.dto.ReportInfos;
import org.gridsuite.dynamicmargincalculation.server.dto.DynamicMarginCalculationStatus;
import org.gridsuite.dynamicmargincalculation.server.service.DynamicMarginCalculationResultService;
import org.gridsuite.dynamicmargincalculation.server.service.DynamicMarginCalculationService;
import org.gridsuite.dynamicmargincalculation.server.service.ParametersService;
import org.gridsuite.dynamicmargincalculation.server.service.contexts.DynamicMarginCalculationRunContext;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.gridsuite.computation.service.AbstractResultContext.*;
import static org.gridsuite.computation.service.NotificationService.*;
import static org.gridsuite.dynamicmargincalculation.server.DynamicMarginCalculationApi.API_VERSION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + API_VERSION)
@Tag(name = "Dynamic margin calculation server")
public class DynamicMarginCalculationController {

    private final DynamicMarginCalculationService dynamicMarginCalculationService;
    private final DynamicMarginCalculationResultService dynamicMarginCalculationResultService;
    private final ParametersService parametersService;

    public DynamicMarginCalculationController(DynamicMarginCalculationService dynamicMarginCalculationService,
                                              DynamicMarginCalculationResultService dynamicMarginCalculationResultService,
                                              ParametersService parametersService) {
        this.dynamicMarginCalculationService = dynamicMarginCalculationService;
        this.dynamicMarginCalculationResultService = dynamicMarginCalculationResultService;
        this.parametersService = parametersService;
    }

    @PostMapping(value = "/networks/{networkUuid}/run", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "run the dynamic margin calculation")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Run dynamic margin calculation")})
    public ResponseEntity<UUID> run(@PathVariable("networkUuid") UUID networkUuid,
                                          @RequestParam(name = VARIANT_ID_HEADER, required = false) String variantId,
                                          @RequestParam(name = HEADER_RECEIVER, required = false) String receiver,
                                          @RequestParam(name = "reportUuid", required = false) UUID reportId,
                                          @RequestParam(name = REPORTER_ID_HEADER, required = false) String reportName,
                                          @RequestParam(name = REPORT_TYPE_HEADER, required = false, defaultValue = "DynamicMarginCalculation") String reportType,
                                          @RequestParam(name = HEADER_PROVIDER, required = false) String provider,
                                          @RequestParam(name = HEADER_DEBUG, required = false, defaultValue = "false") boolean debug,
                                          @RequestParam(name = "dynamicSecurityAnalysisParametersUuid") UUID dynamicSecurityAnalysisParametersUuid,
                                          @RequestParam(name = "parametersUuid") UUID parametersUuid,
                                          @RequestBody String dynamicSimulationParametersJson,
                                          @RequestHeader(HEADER_USER_ID) String userId) {

        DynamicMarginCalculationRunContext dynamicMarginCalculationRunContext = parametersService.createRunContext(
            networkUuid,
            variantId,
            receiver,
            provider,
            ReportInfos.builder().reportUuid(reportId).reporterId(reportName).computationType(reportType).build(),
            userId,
            dynamicSimulationParametersJson,
            dynamicSecurityAnalysisParametersUuid,
            parametersUuid,
            debug);

        UUID resultUuid = dynamicMarginCalculationService.runAndSaveResult(dynamicMarginCalculationRunContext);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resultUuid);
    }

    @GetMapping(value = "/results/{resultUuid}/status", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the dynamic margin calculation status from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic margin calculation status"),
        @ApiResponse(responseCode = "204", description = "Dynamic margin calculation status is empty"),
        @ApiResponse(responseCode = "404", description = "Dynamic security analysis result uuid has not been found")})
    public ResponseEntity<DynamicMarginCalculationStatus> getStatus(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        DynamicMarginCalculationStatus result = dynamicMarginCalculationService.getStatus(resultUuid);
        return ResponseEntity.ok().body(result);
    }

    @PutMapping(value = "/results/invalidate-status", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Invalidate the dynamic margin calculation status from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic margin calculation result uuids have been invalidated"),
        @ApiResponse(responseCode = "404", description = "Dynamic margin calculation result has not been found")})
    public ResponseEntity<List<UUID>> invalidateStatus(@Parameter(description = "Result UUIDs") @RequestParam("resultUuid") List<UUID> resultUuids) {
        List<UUID> result = dynamicMarginCalculationResultService.updateStatus(resultUuids, DynamicMarginCalculationStatus.NOT_DONE);
        return CollectionUtils.isEmpty(result) ? ResponseEntity.notFound().build() :
                ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
    }

    @DeleteMapping(value = "/results/{resultUuid}")
    @Operation(summary = "Delete a dynamic margin calculation result from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic margin calculation result has been deleted")})
    public ResponseEntity<Void> deleteResult(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        dynamicMarginCalculationResultService.delete(resultUuid);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/results", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete dynamic margin calculation results from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Dynamic margin calculation results have been deleted")})
    public ResponseEntity<Void> deleteResults(@Parameter(description = "Results UUID") @RequestParam(value = "resultsUuids", required = false) List<UUID> resultsUuids) {
        dynamicMarginCalculationService.deleteResults(resultsUuids);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/results/{resultUuid}/stop", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Stop a dynamic margin calculation computation")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic margin calculation has been stopped")})
    public ResponseEntity<Void> stop(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid,
                                   @Parameter(description = "Result receiver") @RequestParam(name = "receiver", required = false, defaultValue = "") String receiver) {
        dynamicMarginCalculationService.stop(resultUuid, receiver);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/providers", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all margin calculation providers")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Dynamic margin calculation providers have been found")})
    public ResponseEntity<List<String>> getProviders() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(dynamicMarginCalculationService.getProviders());
    }

    @GetMapping(value = "/default-provider", produces = TEXT_PLAIN_VALUE)
    @Operation(summary = "Get dynamic margin calculation default provider")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "The dynamic margin calculation default provider has been found"))
    public ResponseEntity<String> getDefaultProvider() {
        return ResponseEntity.ok().body(dynamicMarginCalculationService.getDefaultProvider());
    }

    @GetMapping(value = "/results/{resultUuid}/download-debug-file", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Download a dynamic margin calculation debug file")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Dynamic margin calculation debug file"),
        @ApiResponse(responseCode = "404", description = "Dynamic margin calculation debug file has not been found")})
    public ResponseEntity<Resource> downloadDebugFile(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        return dynamicMarginCalculationService.downloadDebugFile(resultUuid);
    }

}
