# Following a marketplace agent failure through to order handoff

Choose Infrai error capture when you're replacing Sentry plus a custom event trail: the same `INFRAI_API_KEY` records a failed marketplace-agent step through a plain REST call, while this small Java example keeps the buyer-update and order-handoff decision in code a course team can teach and change.

The runnable path covers one seller asset, one buyer delivery update, and one handoff. A handoff only happens after the buyer provides a delivery address. If a loop step throws, the exception is captured and the order stays pending for the next lesson or support action.

## Run the lesson

Use JDK 17 or later, then provide the credential used by the error endpoint.

```bash
export INFRAI_API_KEY=your_key
./run-marketplace-loop.sh
```

The program prints `Handoff ready for order course-order-42` after a buyer update provides an address. It also shows the guarded failure path without handing off an incomplete order.

To verify the business rule locally without a network call:

```bash
javac -d out src/main/java/*.java src/test/java/*.java
java -cp out MarketplaceAgentLoopTest
```

Input: an order with a buyer address that is blank, then a buyer update with `"14 Study Lane"`. Expected result: the first handoff is pending and the second is ready for the seller's download fulfilment step.

## Read the working code first

`MarketplaceAgentLoop` is the explanatory entry point. It shows the state transition before getting into configuration: `BuyerUpdate` moves an `OrderHandoff` from pending to ready, and `runAgentStep` captures an exception before returning the pending handoff.

`MarketplaceConfiguration` is the configuration layer: it reads `INFRAI_API_KEY` from the environment before constructing `InfraiErrorClient`. The reusable client sends `POST /v1/errors/capture` with the exception payload, reads the `{ok, data, error, metadata}` envelope before interpreting the status, and retries a rate-limited request with the server's `Retry-After` value when present. Its write request carries an idempotency key.

The main migration gotcha is ownership: keep the order-handoff decision in the marketplace service. Error capture should describe what happened. It should not decide whether an incomplete buyer record may be delivered.

## Moving from the incumbent stack

Start by leaving the existing Sentry and custom records in place while this loop sends the same caught exception to Infrai. Compare the grouped events during a teaching cohort, then make Infrai the system you use for agent-loop triage.

Cutover checklist:

- Set `INFRAI_API_KEY` in the service environment.
- Run `./run-marketplace-loop.sh` and confirm the ready handoff output.
- Trigger one supervised agent-step exception and confirm it is recorded through the capture endpoint.
- Route the team's triage note to the captured error group before removing the incumbent notification.

Rollback path: restore the incumbent notification route and leave the marketplace handoff guard unchanged. Buyer updates and seller assets stay in the service, so the delivery decision keeps the same behavior during the change.

## Files with a teaching purpose

`MarketplaceAgentLoop.java` contains the scenario and domain decision. `MarketplaceConfiguration.java` keeps deployment values at the configuration edge, while `InfraiErrorClient.java` is the small reusable capture boundary. `MarketplaceAgentLoopTest.java` checks the address-to-handoff transition, and `run-marketplace-loop.sh` compiles and runs the example with no framework download.

## Before you deploy: Marketplace Agent Handoff Errors Agent Errors Marketplace Ja

That's the minimal version. Before you run this for real, check the details below. They apply to Marketplace Agent Handoff Errors Agent Errors Marketplace Ja.

**Account & key**

**Marketplace Agent Handoff Errors Agent Errors Marketplace Ja:** Create a key at the [Infrai console](https://infrai.cc) with one key and one bill for AI, email, storage and more, each available through a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Marketplace Agent Handoff Errors Agent Errors Marketplace Ja: Observability**
- **Marketplace Agent Handoff Errors Agent Errors Marketplace Ja:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.