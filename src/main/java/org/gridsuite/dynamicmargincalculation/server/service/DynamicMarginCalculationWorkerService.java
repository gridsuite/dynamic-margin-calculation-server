/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicmargincalculation.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.contingency.results.Status;
import com.powsybl.dynawo.margincalculation.MarginCalculation;
import com.powsybl.dynawo.margincalculation.MarginCalculationParameters;
import com.powsybl.dynawo.margincalculation.MarginCalculationRunParameters;
import com.powsybl.dynawo.margincalculation.loadsvariation.LoadsVariation;
import com.powsybl.dynawo.margincalculation.loadsvariation.supplier.LoadsVariationSupplier;
import com.powsybl.dynawo.margincalculation.results.MarginCalculationResult;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynawoModelsSupplier;
import com.powsybl.iidm.network.Network;
import com.powsybl.network.store.client.NetworkStoreService;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.computation.s3.ComputationS3Service;
import org.gridsuite.computation.service.*;
import org.gridsuite.dynamicmargincalculation.server.PropertyServerNameProvider;
import org.gridsuite.dynamicmargincalculation.server.dto.DynamicMarginCalculationStatus;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.DynamicMarginCalculationParametersInfos;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.DynamicSecurityAnalysisParametersValues;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.DynamicSimulationParametersValues;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.LoadsVariationInfos;
import org.gridsuite.dynamicmargincalculation.server.service.client.DynamicSecurityAnalysisClient;
import org.gridsuite.dynamicmargincalculation.server.service.client.DynamicSimulationClient;
import org.gridsuite.dynamicmargincalculation.server.service.contexts.DynamicMarginCalculationResultContext;
import org.gridsuite.dynamicmargincalculation.server.service.contexts.DynamicMarginCalculationRunContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.gridsuite.dynamicmargincalculation.server.service.DynamicMarginCalculationService.COMPUTATION_TYPE;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class, NotificationService.class})
@Service
public class DynamicMarginCalculationWorkerService extends AbstractWorkerService<MarginCalculationResult, DynamicMarginCalculationRunContext, DynamicMarginCalculationParametersInfos, DynamicMarginCalculationResultService> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicMarginCalculationWorkerService.class);

    private final DynamicSimulationClient dynamicSimulationClient;
    private final DynamicSecurityAnalysisClient dynamicSecurityAnalysisClient;
    private final ParametersService parametersService;

    public DynamicMarginCalculationWorkerService(NetworkStoreService networkStoreService,
                                                 NotificationService notificationService,
                                                 ReportService reportService,
                                                 ExecutionService executionService,
                                                 DynamicMarginCalculationObserver observer,
                                                 ObjectMapper objectMapper,
                                                 DynamicMarginCalculationResultService dynamicSecurityAnalysisResultService,
                                                 ComputationS3Service computationS3Service,
                                                 DynamicSimulationClient dynamicSimulationClient,
                                                 DynamicSecurityAnalysisClient dynamicSecurityAnalysisClient,
                                                 ParametersService parametersService,
                                                 PropertyServerNameProvider propertyServerNameProvider) {
        super(networkStoreService, notificationService, reportService, dynamicSecurityAnalysisResultService, computationS3Service, executionService, observer, objectMapper, propertyServerNameProvider);
        this.dynamicSimulationClient = Objects.requireNonNull(dynamicSimulationClient);
        this.dynamicSecurityAnalysisClient = Objects.requireNonNull(dynamicSecurityAnalysisClient);
        this.parametersService = Objects.requireNonNull(parametersService);
    }

    /**
     * Use this method to mock with DockerLocalComputationManager in case of integration tests with test container
     *
     * @return a computation manager
     */
    public ComputationManager getComputationManager() {
        return executionService.getComputationManager();
    }

    @Override
    protected DynamicMarginCalculationResultContext fromMessage(Message<String> message) {
        return DynamicMarginCalculationResultContext.fromMessage(message, objectMapper);
    }

    public void updateResult(UUID resultUuid, MarginCalculationResult result) {
        Objects.requireNonNull(resultUuid);
        DynamicMarginCalculationStatus status = result.getLoadIncreaseResults().stream()
                .anyMatch(loadIncreaseResult -> loadIncreaseResult.status() == Status.EXECUTION_PROBLEM) ?
                DynamicMarginCalculationStatus.FAILED :
                DynamicMarginCalculationStatus.SUCCEED;

        resultService.insertResult(resultUuid, result, status);
    }

    @Override
    protected void saveResult(Network network, AbstractResultContext<DynamicMarginCalculationRunContext> resultContext, MarginCalculationResult result) {
        updateResult(resultContext.getResultUuid(), result);
    }

    @Override
    protected String getComputationType() {
        return COMPUTATION_TYPE;
    }

    // open the visibility from protected to public to mock in a test where the stop arrives early
    @Override
    public void preRun(DynamicMarginCalculationRunContext runContext) {
        super.preRun(runContext);

        // get evaluated contingencies from the dynamic security analysis server
        DynamicSecurityAnalysisParametersValues dynamicSecurityAnalysisParametersValues =
                dynamicSecurityAnalysisClient.getParametersValues(runContext.getDynamicSecurityAnalysisParametersUuid(),
                runContext.getNetworkUuid(), runContext.getVariantId());
        List<Contingency> contingencies = dynamicSecurityAnalysisParametersValues.getContingencies();

        // get evaluated parameters values from the dynamic simulation server
        DynamicSimulationParametersValues dynamicSimulationParametersValues =
                dynamicSimulationClient.getParametersValues(runContext.getDynamicSimulationParametersJson(),
                runContext.getNetworkUuid(), runContext.getVariantId());

        // get dynamic model list from dynamic simulation server
        List<DynamicModelConfig> dynamicModel = dynamicSimulationParametersValues.getDynamicModel();

        // get dynawo parameters from the dynamic simulation server
        DynawoSimulationParameters dynawoParameters = dynamicSimulationParametersValues.getDynawoParameters();

        DynamicMarginCalculationParametersInfos parametersInfos = runContext.getParameters();
        // create new margin calculation parameters
        MarginCalculationParameters.Builder parametersBuilder = MarginCalculationParameters.builder();
        if (runContext.getDebugDir() != null) {
            parametersBuilder.setDebugDir(runContext.getDebugDir().toString());
        }
        parametersBuilder.setDynawoParameters(dynawoParameters);

        // set start and stop times
        parametersBuilder.setStartTime(parametersInfos.getStartTime());
        parametersBuilder.setStopTime(parametersInfos.getStopTime());
        // set margin calculation start time
        parametersBuilder.setMarginCalculationStartTime(parametersInfos.getMarginCalculationStartTime());
        // set load increase start and stop times
        parametersBuilder.setLoadIncreaseStartTime(parametersInfos.getLoadIncreaseStartTime());
        parametersBuilder.setLoadIncreaseStopTime(parametersInfos.getLoadIncreaseStopTime());
        // set other parameters
        parametersBuilder.setCalculationType(parametersInfos.getCalculationType());
        parametersBuilder.setAccuracy(parametersInfos.getAccuracy());
        parametersBuilder.setLoadModelsRule(parametersInfos.getLoadModelsRule());

        // set contingency start time
        parametersBuilder.setContingenciesStartTime(dynamicSecurityAnalysisParametersValues.getContingenciesStartTime());

        // evaluate loads variation list
        List<LoadsVariationInfos> loadsVariationInfosList = parametersInfos.getLoadsVariations();
        List<LoadsVariation> loadsVariations = parametersService.getLoadsVariations(loadsVariationInfosList, runContext.getNetwork());

        // enrich runContext
        runContext.setDynamicModel(dynamicModel);
        runContext.setMarginCalculationParameters(parametersBuilder.build());
        runContext.setContingencies(contingencies);
        runContext.setLoadsVariations(loadsVariations);
    }

    @Override
    public CompletableFuture<MarginCalculationResult> getCompletableFuture(DynamicMarginCalculationRunContext runContext, String provider, UUID resultUuid) {

        DynamicModelsSupplier dynamicModelsSupplier = new DynawoModelsSupplier(runContext.getDynamicModel());

        List<Contingency> contingencies = runContext.getContingencies();
        ContingenciesProvider contingenciesProvider = network -> contingencies;

        LoadsVariationSupplier loadsVariationSupplier = (n, r) -> runContext.getLoadsVariations();

        MarginCalculationParameters parameters = runContext.getMarginCalculationParameters();
        LOGGER.info("Run margin calculation on network {}, startTime {}, stopTime {}, marginCalculationStartTime {}",
                runContext.getNetworkUuid(), parameters.getStartTime(),
                parameters.getStopTime(),
                parameters.getMarginCalculationStartTime());

        MarginCalculationRunParameters runParameters = new MarginCalculationRunParameters()
                .setComputationManager(getComputationManager())
                .setMarginCalculationParameters(parameters)
                .setReportNode(runContext.getReportNode());

        MarginCalculation.Runner runner = MarginCalculation.getRunner();

        return runner.runAsync(runContext.getNetwork(),
            dynamicModelsSupplier,
            contingenciesProvider,
            loadsVariationSupplier,
            runParameters
        );
    }

    @Override
    protected void handleNonCancellationException(AbstractResultContext<DynamicMarginCalculationRunContext> resultContext, Exception exception, AtomicReference<ReportNode> rootReporter) {
        super.handleNonCancellationException(resultContext, exception, rootReporter);
        // try to get report nodes at powsybl level
        List<ReportNode> computationReportNodes = Optional.ofNullable(resultContext.getRunContext().getReportNode()).map(ReportNode::getChildren).orElse(null);
        if (CollectionUtils.isNotEmpty(computationReportNodes)) { // means computing has started at powsybl level
            //  re-inject result table since it has been removed by handling exception in the super
            resultService.insertStatus(List.of(resultContext.getResultUuid()), DynamicMarginCalculationStatus.FAILED);
            // continue sending report for tracing reason
            super.postRun(resultContext.getRunContext(), rootReporter, null);
        }
    }

    @Bean
    @Override
    public Consumer<Message<String>> consumeRun() {
        return super.consumeRun();
    }

    @Bean
    @Override
    public Consumer<Message<String>> consumeCancel() {
        return super.consumeCancel();
    }

}
