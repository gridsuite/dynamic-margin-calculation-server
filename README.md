# Dynamic Margin Calculation Server

[![Actions Status](https://github.com/gridsuite/dynamic-margin-calculation-server/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/gridsuite/dynamic-margin-calculation-server/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=org.gridsuite%3Adynamic-margin-calculation-server&metric=coverage)](https://sonarcloud.io/component_measures?id=org.gridsuite%3Adynamic-margin-calculation-server&metric=coverage)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

## Description

The **dynamic-margin-calculation-server** is a microservice of the [GridSuite](https://github.com/gridsuite) platform dedicated to **dynamic margin calculation computation**.

Margin calculation evaluates, through successive load increases, the maximum load level a network can sustain before a set of contingencies (evaluated by dynamic security analysis) leads to a critical loss of stability. It relies on the [dynamic-simulation-server](https://github.com/gridsuite/dynamic-simulation-server) (dynamic model, Dynawo parameters) and the [dynamic-security-analysis-server](https://github.com/gridsuite/dynamic-security-analysis-server) (contingencies) as inputs, then runs the margin calculation using a Dynawo-based provider.

It provides the following capabilities:

- **Run dynamic margin calculation computations** from a network, using the dynamic model and Dynawo parameters retrieved from the dynamic-simulation-server and the contingencies retrieved from the dynamic-security-analysis-server.
- **Manage parameter sets** including start/stop times, load increase times, calculation type, accuracy and loads variation configuration.
- **Track computation status** and invalidate/stop running computations.
- **Store detailed computation results** (per load-increase scenario, including failed criteria) in the database; the computation status is exposed through the REST API, while the structured result content itself is not exposed through a dedicated REST endpoint.
- **Delete results** individually or in bulk.
- **Download debug files** produced by the underlying margin calculation provider.
- Run computations **asynchronously** (via a RabbitMQ message queue).

---

## Technical Stack

- Spring Boot (Web, Data JPA, Actuator, Cloud Stream)
- PostgreSQL
- Liquibase
- RabbitMQ via Spring Cloud Stream
- API documentation: OpenAPI / Swagger (`springdoc`)
- Micrometer / Prometheus
- [gridsuite-computation](https://github.com/gridsuite/computation)
- [powsybl-dynawo](https://github.com/powsybl/powsybl-dynawo) (Dynawo margin calculation provider)

---

## Development Scripts

Build Docker image

```shell
mvn install -DskipTests -Dpowsybl.docker.install
```

Please read [liquibase usage](https://github.com/powsybl/powsybl-parent/#liquibase-usage) for instructions to automatically generate changesets. After you generated a changeset do not forget to add it to git and in `src/main/resources/db/changelog/db.changelog-master.yaml`.

---

## Interactions with Other Microservices

```text
┌────────────────────────────────────┐
│  dynamic-margin-calculation-server │──► network-store-server            (read network topology)
│                                    │──► directory-server                (resolve element names)
│                                    │──► dynamic-simulation-server       (fetch dynamic model and Dynawo parameters)
│                                    │──► dynamic-security-analysis-server (fetch evaluated contingencies)
│                                    │──► report-server                  (post computation functional logs)
└────────────────────────────────────┘
          ▲  ▼
       RabbitMQ (dmc.run / dmc.cancel / dmc.result / dmc.stopped / dmc.cancelfailed / dmc.debug)
```

---

## Asynchronous Execution Flow

1. The controller publishes a message on the `dmc.run` queue.
2. Parallel consumers (`consumeRun1`, `consumeRun2`) process messages concurrently for load balancing.
3. Before running, contingencies are fetched from the dynamic-security-analysis-server and the dynamic model / Dynawo parameters from the dynamic-simulation-server.
4. The computation result is published on `dmc.result`.
5. Cancellation of a running computation goes through the `dmc.cancel` queue.
6. Debug information (when debug mode is enabled) is published on `dmc.debug`.
7. Dead-letter queues (`dmc.run.dlx`) and quorum queues ensure reliability.

---

## Parameters

Dynamic margin calculation parameters include:

- **Provider** selection (Dynawo).
- Start/stop times, margin calculation start time, load increase start/stop times.
- Calculation type, accuracy, load models rule.
- **Loads variation** configuration (identifiables and variation type), persisted per parameter set.

Parameter sets can be created, duplicated, updated, retrieved and deleted through the REST API.

---

## Built on gridsuite-computation

The following capabilities are provided by the gridsuite-computation shared library:

- asynchronous run/cancel pipeline,
- transactional result notifications,
- report integration,
- Micrometer observability.

The dynamic-margin-calculation-server itself focuses on margin-calculation-specific logic (parameters, loads variation resolution, retrieval of dynamic model/parameters/contingencies from other servers, provider integration) and delegates the common computation infrastructure to this lib.

---

## Useful Links

You can find [information on Dynawo here](https://dynawo.github.io/).
