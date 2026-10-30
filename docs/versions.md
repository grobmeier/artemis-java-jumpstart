# Versions and sources

Baseline source review: **29 October 2026**, Java **21** without preview. Pins are unchanged by the English migration. Artemis Java packages retain org.apache.activemq.artemis while Maven coordinates use org.apache.artemis. These are recorded baseline decisions, not a claim that a future latest release was checked.

| Component | Pinned version | Primary source / decision |
|---|---|---|
| Apache Artemis Server + Jakarta Client | 2.57.0 | [Download and Java compatibility](https://artemis.apache.org/components/artemis/download/), [Central metadata](https://repo.maven.apache.org/maven2/org/apache/artemis/artemis-server/maven-metadata.xml) |
| Jakarta Messaging API | 3.1.0 | [Jakarta Messaging 3.1](https://jakarta.ee/specifications/messaging/3.1/); Java SE 11+, jakarta.jms |
| Apache Maven | 3.9.16 | [Maven Release History](https://maven.apache.org/docs/history.html); maintained 3.9 line |
| Maven Wrapper | 3.3.4, binary type | [Wrapper-Download](https://maven.apache.org/tools/wrapper/download.cgi), [Wrapper documentation](https://maven.apache.org/tools/wrapper/); bootstrap JAR included |
| Jackson Databind | 2.21.8 | [2.21.8 Release](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.21.8); 2.21 LTS for records/JSON, no polymorphic deserialization |
| H2 | 2.5.252 | [H2 Download](https://h2database.com/html/download.html); embedded local S09 effect only |
| SLF4J Simple | 2.0.17 | [SLF4J Release Notes](https://www.slf4j.org/news.html); compact provider diagnostics |
| JUnit Jupiter | 5.13.4 | [JUnit Release Notes](https://docs.junit.org/5.13.4/release-notes/); process/TCP tests |


SLF4J API resolves to 2.0.18 through the pinned Artemis tree; Simple is 2.0.17 on the compatible 2.0 provider line. See the full [resolved dependency/licence inventory](../licenses/DEPENDENCIES.md). Direct dependencies have exact parent-POM pins; transitive dependencies follow their published POMs, without LATEST or dynamic ranges. Recheck the actual dependency tree when upgrading.

Build plugin pins: clean 3.4.1, resources 3.3.1, compiler 3.14.1, surefire/failsafe 3.5.4, jar 3.4.2, install/deploy 3.1.4, shade 3.6.1, assembly 3.7.1. Wrapper generation used maven-wrapper-plugin:3.3.4:wrapper -Dtype=bin -Dmaven=3.9.16. Wrapper properties pin SHA-256 for the Maven distribution and bootstrap JAR.

## Behavior references

The baseline Artemis manual identified itself as 2.57.0. Links to latest may move; recheck version and behavior on upgrades.

- [Embedding](https://artemis.apache.org/components/artemis/documentation/latest/embedding-artemis.html): explicit persistent configuration in a separate process.
- [Jakarta Messaging](https://artemis.apache.org/components/artemis/documentation/latest/using-jms.html): direct client construction without JNDI.
- [Security](https://artemis.apache.org/components/artemis/documentation/latest/security.html), [TLS/transports](https://artemis.apache.org/components/artemis/documentation/latest/configuring-transports.html): namespace roles/FQQN, hostname checks and no trainee management.
- [Redelivery/DLQ](https://artemis.apache.org/components/artemis/documentation/latest/undelivered-messages.html), [expiry](https://artemis.apache.org/components/artemis/documentation/latest/message-expiry.html), [groups](https://artemis.apache.org/components/artemis/documentation/latest/message-grouping.html): attempt limits, error resources and assignment.
- [Flow control](https://artemis.apache.org/components/artemis/documentation/latest/flow-control.html), [paging](https://artemis.apache.org/components/artemis/documentation/latest/paging.html): demand-driven consumption and bounded memory/PAGE. Producer credit window is -1, persistent sends synchronous with response deadlines and bounded CLI load.
- [Jakarta Messaging 3.1 specification](https://jakarta.ee/specifications/messaging/3.1/jakarta-messaging-spec-3.1.html): acknowledgement, delivery delay, shared durable subscriptions and correlation. Delay is also verified against the packaged client/broker.

GitHub Actions retain pinned commits: [checkout v7.0.1](https://github.com/actions/checkout/releases/tag/v7.0.1) (3d3c42e5aac5ba805825da76410c181273ba90b1), [setup-java v6.1.0](https://github.com/actions/setup-java/releases/tag/v6.1.0) (b843ebf9b913146b38da2eaa7aeddf76074ae1ad), [upload-artifact v7.0.2](https://github.com/actions/upload-artifact/releases/tag/v7.0.2) (cf430e030ddbb5b0abf93d22962f4752f3646cd9).

Source review is distinct from executed platform verification. See [acceptance.md](acceptance.md).
