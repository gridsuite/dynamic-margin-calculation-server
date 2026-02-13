/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicmargincalculation.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.datasource.ResourceDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.contingency.Contingency;
import com.powsybl.dynawo.contingency.results.FailedCriterion;
import com.powsybl.dynawo.contingency.results.ScenarioResult;
import com.powsybl.dynawo.margincalculation.MarginCalculation;
import com.powsybl.dynawo.margincalculation.results.LoadIncreaseResult;
import com.powsybl.dynawo.margincalculation.results.MarginCalculationResult;
import com.powsybl.iidm.network.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.PreloadingStrategy;
import org.gridsuite.computation.service.NotificationService;
import org.gridsuite.dynamicmargincalculation.server.dto.DynamicMarginCalculationStatus;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.DynamicMarginCalculationParametersInfos;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.DynamicSecurityAnalysisParametersValues;
import org.gridsuite.dynamicmargincalculation.server.dto.parameters.DynamicSimulationParametersValues;
import org.gridsuite.dynamicmargincalculation.server.entities.parameters.DynamicMarginCalculationParametersEntity;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.messaging.Message;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.powsybl.dynawo.contingency.results.Status.CONVERGENCE;
import static com.powsybl.dynawo.contingency.results.Status.CRITERIA_NON_RESPECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.gridsuite.computation.s3.ComputationS3Service.METADATA_FILE_NAME;
import static org.gridsuite.computation.service.AbstractResultContext.REPORTER_ID_HEADER;
import static org.gridsuite.computation.service.AbstractResultContext.VARIANT_ID_HEADER;
import static org.gridsuite.computation.service.NotificationService.*;
import static org.gridsuite.dynamicmargincalculation.server.controller.utils.TestUtils.RESOURCE_PATH_DELIMITER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicMarginCalculationControllerTest extends AbstractDynamicMarginCalculationControllerTest {

    // directories
    public static final String DATA_IEEE14_BASE_DIR = RESOURCE_PATH_DELIMITER + "data" + RESOURCE_PATH_DELIMITER + "ieee14";

    private static final UUID NETWORK_UUID = UUID.randomUUID();
    private static final String VARIANT_1_ID = "variant_1";
    private static final String NETWORK_FILE = "IEEE14.iidm";

    private static final UUID PARAMETERS_UUID = UUID.randomUUID();
    private static final UUID DSA_PARAMETERS_UUID = UUID.randomUUID();

    private static final String LINE_ID = "_BUS____1-BUS____5-1_AC";
    private static final String GEN_ID = "_GEN____2_SM";

    @Autowired
    private OutputDestination output;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoSpyBean
    private NotificationService notificationService;

    @MockitoSpyBean
    private S3Client s3Client;

    @Override
    public OutputDestination getOutputDestination() {
        return output;
    }

    @Override
    protected void initParametersRepositoryMock() {
        DynamicMarginCalculationParametersInfos params = parametersService.getDefaultParametersValues("Dynawo");
        params.setLoadsVariations(List.of()); // keep test independent from directory/filter enrichment

        DynamicMarginCalculationParametersEntity entity = new DynamicMarginCalculationParametersEntity(params);
        given(dynamicMarginCalculationParametersRepository.findById(PARAMETERS_UUID)).willReturn(Optional.of(entity));
    }

    @Override
    protected void initExternalClientsMocks() {
        // Mock for network
        ReadOnlyDataSource dataSource = new ResourceDataSource("IEEE14",
                new ResourceSet(DATA_IEEE14_BASE_DIR, NETWORK_FILE));
        Network network = Importers.importData("XIIDM", dataSource, null);
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_1_ID);
        given(networkStoreClient.getNetwork(NETWORK_UUID, PreloadingStrategy.COLLECTION)).willReturn(network);

        // Mock for dynamic security analysis
        when(dynamicSecurityAnalysisClient.getParametersValues(eq(DSA_PARAMETERS_UUID), eq(NETWORK_UUID), any()))
                .thenReturn(DynamicSecurityAnalysisParametersValues.builder()
                        .contingenciesStartTime(105d)
                        .contingencies(List.of(Contingency.load("_LOAD__11_EC")))
                        .build());

        // Mock for dynamic simulation server
        when(dynamicSimulationClient.getParametersValues(anyString(), eq(NETWORK_UUID), any()))
                .thenReturn(DynamicSimulationParametersValues.builder().build());
    }

    @Test
    void testResult() throws Exception {

        // mock DynamicMarginCalculationWorkerService
        doReturn(CompletableFuture.completedFuture(MarginCalculationResult.empty()))
                .when(dynamicMarginCalculationWorkerService).getCompletableFuture(any(), any(), any());

        // mock s3 client for run with debug
        doReturn(PutObjectResponse.builder().build())
                .when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        doReturn(new ResponseInputStream<>(
                GetObjectResponse.builder()
                        .metadata(Map.of(METADATA_FILE_NAME, "debugFile"))
                        .contentLength(100L).build(),
                AbortableInputStream.create(new ByteArrayInputStream("s3 debug file content".getBytes()))
        )).when(s3Client).getObject(any(GetObjectRequest.class));

        // run with debug (body is the dynamicSimulationParametersJson string)
        String dynamicSimulationParametersJson = "{}";
        MvcResult result = mockMvc.perform(
                        post("/v1/networks/{networkUuid}/run", NETWORK_UUID)
                                .param(VARIANT_ID_HEADER, VARIANT_1_ID)
                                .param("dynamicSecurityAnalysisParametersUuid", DSA_PARAMETERS_UUID.toString())
                                .param("parametersUuid", PARAMETERS_UUID.toString())
                                .param(HEADER_DEBUG, "true")
                                .contentType(APPLICATION_JSON)
                                .content(dynamicSimulationParametersJson)
                                .header(HEADER_USER_ID, "testUserId")
                )
                .andExpect(status().isOk())
                .andReturn();

        UUID runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        // check notification of result
        Message<byte[]> messageSwitch = output.receive(10_000, dmcResultDestination);
        assertThat(messageSwitch).isNotNull();
        assertThat(messageSwitch.getHeaders()).containsEntry(HEADER_RESULT_UUID, runUuid.toString());

        // check notification of debug
        messageSwitch = output.receive(10_000, dmcDebugDestination);
        assertThat(messageSwitch).isNotNull();
        assertThat(messageSwitch.getHeaders()).containsEntry(HEADER_RESULT_UUID, runUuid.toString());

        // download debug zip file is ok
        mockMvc.perform(get("/v1/results/{resultUuid}/download-debug-file", runUuid))
                .andExpect(status().isOk());

        // check interaction with s3 client
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class));

        // run on implicit default variant (variantId omitted)
        result = mockMvc.perform(
                        post("/v1/networks/{networkUuid}/run", NETWORK_UUID)
                                .param("dynamicSecurityAnalysisParametersUuid", DSA_PARAMETERS_UUID.toString())
                                .param("parametersUuid", PARAMETERS_UUID.toString())
                                .contentType(APPLICATION_JSON)
                                .content(dynamicSimulationParametersJson)
                                .header(HEADER_USER_ID, "testUserId")
                )
                .andExpect(status().isOk())
                .andReturn();

        runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        messageSwitch = output.receive(10_000, dmcResultDestination);
        assertThat(messageSwitch).isNotNull();
        assertThat(messageSwitch.getHeaders()).containsEntry(HEADER_RESULT_UUID, runUuid.toString());

        // get status (depending on timing, could be RUNNING or SUCCEED)
        result = mockMvc.perform(get("/v1/results/{resultUuid}/status", runUuid))
                .andExpect(status().isOk())
                .andReturn();

        DynamicMarginCalculationStatus statusValue = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DynamicMarginCalculationStatus.class
        );
        assertThat(statusValue).isIn(DynamicMarginCalculationStatus.SUCCEED, DynamicMarginCalculationStatus.RUNNING);

        // status of non-existing result => empty (mapped to null in helper)
        assertResultStatus(UUID.randomUUID(), null);

        // invalidate status => set NOT_DONE
        mockMvc.perform(put("/v1/results/invalidate-status")
                        .param("resultUuid", runUuid.toString()))
                .andExpect(status().isOk());

        result = mockMvc.perform(get("/v1/results/{resultUuid}/status", runUuid))
                .andExpect(status().isOk())
                .andReturn();

        DynamicMarginCalculationStatus statusAfterInvalidate = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DynamicMarginCalculationStatus.class
        );
        assertThat(statusAfterInvalidate).isSameAs(DynamicMarginCalculationStatus.NOT_DONE);

        // invalidate status for unknown result => 404 (controller returns notFound when update list is empty)
        mockMvc.perform(put("/v1/results/invalidate-status")
                        .param("resultUuid", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());

        // delete one result
        mockMvc.perform(delete("/v1/results/{resultUuid}", runUuid))
                .andExpect(status().isOk());

        // verify deleted => status becomes empty (null)
        assertResultStatus(runUuid, null);

        // delete non-existing => ok
        mockMvc.perform(delete("/v1/results/{resultUuid}", UUID.randomUUID()))
                .andExpect(status().isOk());

        // delete all results => ok
        mockMvc.perform(delete("/v1/results"))
                .andExpect(status().isOk());
    }

    @Test
    void testRunWithSynchronousExceptions() throws Exception {
        String dynamicSimulationParametersJson = "{}";

        // provider not found
        mockMvc.perform(
                        post("/v1/networks/{networkUuid}/run", NETWORK_UUID)
                                .param(HEADER_PROVIDER, "notFoundProvider")
                                .param(VARIANT_ID_HEADER, VARIANT_1_ID)
                                .param("dynamicSecurityAnalysisParametersUuid", DSA_PARAMETERS_UUID.toString())
                                .param("parametersUuid", PARAMETERS_UUID.toString())
                                .contentType(APPLICATION_JSON)
                                .content(dynamicSimulationParametersJson)
                                .header(HEADER_USER_ID, "testUserId")
                )
                .andExpect(status().isNotFound());

        // parameters not found
        mockMvc.perform(
                        post("/v1/networks/{networkUuid}/run", NETWORK_UUID)
                                .param(VARIANT_ID_HEADER, VARIANT_1_ID)
                                .param("dynamicSecurityAnalysisParametersUuid", DSA_PARAMETERS_UUID.toString())
                                .param("parametersUuid", UUID.randomUUID().toString())
                                .contentType(APPLICATION_JSON)
                                .content(dynamicSimulationParametersJson)
                                .header(HEADER_USER_ID, "testUserId")
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void testRunWithResult() throws Exception {
        doAnswer(invocation -> null).when(reportService).deleteReport(any());
        doAnswer(invocation -> null).when(reportService).sendReport(any(), any());

        doReturn(CompletableFuture.completedFuture(new MarginCalculationResult(List.of(
                        new LoadIncreaseResult(100, CRITERIA_NON_RESPECTED, List.of(),
                                List.of(new FailedCriterion("total load power = 207.704MW > 200MW (criteria id: Risque protection)", 56.929320))),
                        new LoadIncreaseResult(0, CONVERGENCE,
                                List.of(new ScenarioResult(LINE_ID, CONVERGENCE),
                                        new ScenarioResult(GEN_ID, CONVERGENCE)))
                        ))))
                .when(dynamicMarginCalculationWorkerService).getCompletableFuture(any(), any(), any());
        String dynamicSimulationParametersJson = "{}";

        MvcResult result = mockMvc.perform(
                        post("/v1/networks/{networkUuid}/run", NETWORK_UUID)
                                .param(VARIANT_ID_HEADER, VARIANT_1_ID)
                                .param("dynamicSecurityAnalysisParametersUuid", DSA_PARAMETERS_UUID.toString())
                                .param("parametersUuid", PARAMETERS_UUID.toString())
                                .param("reportUuid", UUID.randomUUID().toString())
                                .param(REPORTER_ID_HEADER, "dmc")
                                .contentType(APPLICATION_JSON)
                                .content(dynamicSimulationParametersJson)
                                .header(HEADER_USER_ID, "testUserId")
                )
                .andExpect(status().isOk())
                .andReturn();

        UUID runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        Message<byte[]> messageSwitch = output.receive(1000, dmcResultDestination);
        assertThat(messageSwitch).isNotNull();
        assertThat(messageSwitch.getHeaders()).containsEntry(HEADER_RESULT_UUID, runUuid.toString());
    }

    // --- BEGIN Test cancelling a running computation ---//

    private void mockSendRunMessage(Supplier<CompletableFuture<?>> runAsyncMock) {
        // In test environment, the test binder calls consumers directly in the caller thread, i.e. the controller thread.
        // By consequence, a real asynchronous Producer/Consumer can not be simulated like prod.
        // So mocking producer in a separated thread differing from the controller thread (same pattern as the selected test).
        doAnswer(invocation -> CompletableFuture.runAsync(() -> {
            // static mock must be in the same thread of the consumer
            // see : https://stackoverflow.com/questions/76406935/mock-static-method-in-spring-boot-integration-test
            try (MockedStatic<MarginCalculation> marginCalculationMockedStatic = mockStatic(MarginCalculation.class)) {
                MarginCalculation.Runner runner = mock(MarginCalculation.Runner.class);
                marginCalculationMockedStatic.when(MarginCalculation::getRunner).thenReturn(runner);

                // This gives us deterministic control over "long" vs "short" computations.
                doAnswer(invocation2 -> runAsyncMock.get())
                        .when(runner).runAsync(any(), any(), any(), any(), any());

                // call real method sendRunMessage
                try {
                    invocation.callRealMethod();
                } catch (Throwable e) {
                    throw new RuntimeException("Error while wrapping sendRunMessage in a separated thread", e);
                }
            }
        }))
        .when(notificationService).sendRunMessage(any());
    }

    private UUID runAndCancel(CountDownLatch cancelLatch, int cancelDelayMs) throws Exception {
        String dynamicSimulationParametersJson = "{}";

        // run the dynamic margin calculation
        MvcResult result = mockMvc.perform(
                        post("/v1/networks/{networkUuid}/run", NETWORK_UUID)
                                .param(VARIANT_ID_HEADER, VARIANT_1_ID)
                                .param("dynamicSecurityAnalysisParametersUuid", DSA_PARAMETERS_UUID.toString())
                                .param("parametersUuid", PARAMETERS_UUID.toString())
                                .contentType(APPLICATION_JSON)
                                .content(dynamicSimulationParametersJson)
                                .header(HEADER_USER_ID, "testUserId"))
                .andExpect(status().isOk())
                .andReturn();

        UUID runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        // Should be running quickly after creation
        assertResultStatus(runUuid, DynamicMarginCalculationStatus.RUNNING);

        // stop, with a timeout to avoid test hangs if an exception occurs before latch countdown
        boolean completed = cancelLatch.await(5, TimeUnit.SECONDS);
        if (!completed) {
            throw new AssertionError("Timed out waiting for cancelLatch, something might have crashed before latch countdown happens.");
        }

        // optional extra wait (simulate user cancelling early/late)
        await().pollDelay(cancelDelayMs, TimeUnit.MILLISECONDS).until(() -> true);

        mockMvc.perform(put("/v1/results/{resultUuid}/stop", runUuid))
                .andExpect(status().isOk());

        return runUuid;
    }

    @Test
    void testStopOnTime() throws Exception {
        CountDownLatch cancelLatch = new CountDownLatch(1);

        // Emit messages in separate threads, like in production.
        mockSendRunMessage(() -> {
            // trigger stop at the beginning of computation
            cancelLatch.countDown();

            // fake a long computation (1s)
            return CompletableFuture.supplyAsync(
                    () -> null,
                    CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS)
            );
        });

        UUID runUuid = runAndCancel(cancelLatch, 0);

        // Must have a cancel message in the stop queue
        Message<byte[]> message = output.receive(1000, dmcStoppedDestination);
        assertThat(message).isNotNull();
        assertThat(message.getHeaders())
                .containsEntry(HEADER_RESULT_UUID, runUuid.toString())
                .containsKey(HEADER_MESSAGE);

        // result has been deleted by cancel so status is empty (null)
        assertResultStatus(runUuid, null);
    }

    @Test
    void testStopEarly() throws Exception {
        CountDownLatch cancelLatch = new CountDownLatch(1);

        // Emit messages in separate threads, like in production.
        mockSendRunMessage(() -> CompletableFuture.supplyAsync(() -> null));

        // Delay before the computation starts to simulate "cancel too early"
        doAnswer(invocation -> {
            Object object = invocation.callRealMethod();

            cancelLatch.countDown();

            await().pollDelay(1000, TimeUnit.MILLISECONDS).until(() -> true);
            return object;
        }).when(dynamicMarginCalculationWorkerService).preRun(any());

        UUID runUuid = runAndCancel(cancelLatch, 0);

        // Must have a cancel failed message
        Message<byte[]> message = output.receive(1000, dmcCancelFailedDestination);
        assertThat(message).isNotNull();
        assertThat(message.getHeaders())
                .containsEntry(HEADER_RESULT_UUID, runUuid.toString())
                .containsKey(HEADER_MESSAGE);

        // cancel failed so result still exists (status remains RUNNING in this behaviour)
        assertResultStatus(runUuid, DynamicMarginCalculationStatus.RUNNING);
    }

    @Test
    void testStopLately() throws Exception {
        CountDownLatch cancelLatch = new CountDownLatch(1);

        // Emit messages in separate threads, like in production.
        mockSendRunMessage(() -> {
            // using latch to trigger stop dynamic margin calculation at the beginning of computation
            cancelLatch.countDown();

            // fake a short computation
            return CompletableFuture.supplyAsync(MarginCalculationResult::empty);
        });

        // test run then cancel
        UUID runUuid = runAndCancel(cancelLatch, 1000);

        // check result
        // Computation finished quickly => must have a result message
        Message<byte[]> message = output.receive(1000, dmcResultDestination);
        assertThat(message).isNotNull();
        assertThat(message.getHeaders())
                .containsEntry(HEADER_RESULT_UUID, runUuid.toString());

        // Stop arrives after end => cancel failed message
        message = output.receive(1000, dmcCancelFailedDestination);
        assertThat(message).isNotNull();
        assertThat(message.getHeaders())
                .containsEntry(HEADER_RESULT_UUID, runUuid.toString())
                .containsKey(HEADER_MESSAGE);

        // cancel failed so results are not deleted
        assertResultStatus(runUuid, DynamicMarginCalculationStatus.SUCCEED);
    }

    // --- END Test cancelling a running computation ---//
}
