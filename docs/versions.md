# Versionen und Quellen

Prüftag: **29. Oktober 2026**. Java-Baseline: **21**, ohne Preview. Artemis **2.57.0** ist laut offizieller Downloadseite die aktuelle stabile Ausgabe und unterstützt Java 17+. Daher ist keine ältere Brokerlinie für Java 21 erforderlich. Maven-Central-Metadaten und erfolgreicher Artefaktbezug bestätigen die Koordinaten `org.apache.artemis`; Java-Packages heißen weiterhin `org.apache.activemq.artemis`.

| Baustein | Festgelegte Version | Primärquelle / Entscheidung |
|---|---|---|
| Apache Artemis Server + Jakarta Client | 2.57.0 | [Download und Java-Kompatibilität](https://artemis.apache.org/components/artemis/download/), [Central-Metadaten](https://repo.maven.apache.org/maven2/org/apache/artemis/artemis-server/maven-metadata.xml) |
| Jakarta Messaging API | 3.1.0 | [Jakarta Messaging 3.1](https://jakarta.ee/specifications/messaging/3.1/); Java SE 11+, jakarta.jms |
| Apache Maven | 3.9.16 | [Maven Release History](https://maven.apache.org/docs/history.html); gepflegte 3.9-Linie |
| Maven Wrapper | 3.3.4, Typ bin | [Wrapper-Download](https://maven.apache.org/tools/wrapper/download.cgi), [Wrapper-Dokumentation](https://maven.apache.org/tools/wrapper/); Bootstrap-JAR bereits enthalten |
| Jackson Databind | 2.21.8 | [2.21.8 Release](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.21.8); gepflegte 2.21-LTS-Linie für kleine Records/JSON, keine polymorphe Objektauflösung |
| H2 | 2.5.252 | [H2 Download](https://h2database.com/html/download.html); nur eingebetteter lokaler S09-Effekt |
| SLF4J Simple | 2.0.17 | [SLF4J Release Notes](https://www.slf4j.org/news.html); kompakte Providerdiagnostik |
| JUnit Jupiter | 5.13.4 | [JUnit Release Notes](https://docs.junit.org/5.13.4/release-notes/); Prozess-/TCP-Tests |

SLF4J API wird durch den festgelegten Artemis-Abhängigkeitsbaum als 2.0.18 aufgelöst; Simple bleibt 2.0.17. Die 2.0-Providerlinie ist kompatibel. Die gesamte aufgelöste Liste steht in [licenses/DEPENDENCIES.md](../licenses/DEPENDENCIES.md).

Alle direkten Dependencies stehen exakt im Parent-POM. Transitive Versionen folgen den veröffentlichten POMs dieser festen Versionen; keine dynamischen Bereiche oder LATEST-Angaben. Dependency-/Lizenzbestand ist im Laufzeit-ZIP unter licenses nach Artefaktversion getrennt enthalten, soweit die Dependency entsprechende Ressourcen mitliefert. Für eine erneute Dependencyprüfung die tatsächlich aufgelöste Maven-Struktur betrachten.

## Build-Plugins

Fest im Parent-POM: clean **3.4.1**, resources **3.3.1**, compiler **3.14.1**, surefire/failsafe **3.5.4**, jar **3.4.2**, install/deploy **3.1.4**, shade **3.6.1**, assembly **3.7.1**. Die Wrapper-Erzeugung wurde ausdrücklich mit `org.apache.maven.plugins:maven-wrapper-plugin:3.3.4:wrapper -Dtype=bin -Dmaven=3.9.16` ausgeführt. Die konkreten Plugin-Artefakte wurden beim Build aus dem konfigurierten Maven-Repository bezogen; offizielle Dokumentation: [Compiler 3.14.1](https://maven.apache.org/components/plugins-archives/maven-compiler-plugin-3.14.1/), [Surefire 3.5.4](https://maven.apache.org/surefire-archives/surefire-3.5.4/), [Shade](https://maven.apache.org/plugins/maven-shade-plugin/), [Assembly](https://maven.apache.org/plugins/maven-assembly-plugin/), [Maven-Pluginverzeichnis](https://maven.apache.org/plugins/).

## Versionsabhängig geprüfte Brokerfunktionen

Die abgerufene `latest`-Dokumentation bezeichnet sich am Prüftag ausdrücklich als Handbuch für **2.57.0**. Diese Links können künftig wechseln; bei Aktualisierung erneut die Versionsangabe und Tests prüfen.

- [Programmatic embedding](https://artemis.apache.org/components/artemis/documentation/latest/embedding-artemis.html): separater Prozess mit expliziter persistenter ConfigurationImpl.
- [Jakarta Messaging](https://artemis.apache.org/components/artemis/documentation/latest/using-jms.html): Jakarta-Client, direkte Instanziierung ohne JNDI.
- [Security](https://artemis.apache.org/components/artemis/documentation/latest/security.html) und [Transport/TLS](https://artemis.apache.org/components/artemis/documentation/latest/configuring-transports.html): Rollen, namespacegebundene Fehleradressen, FQQN, Hostnameprüfung, keine Fernverwaltung für Trainees.
- [Redelivery/DLQ](https://artemis.apache.org/components/artemis/documentation/latest/undelivered-messages.html), [Expiry](https://artemis.apache.org/components/artemis/documentation/latest/message-expiry.html), [Message Groups](https://artemis.apache.org/components/artemis/documentation/latest/message-grouping.html): Limits, erzeugte Fehlerressourcen und Gruppenzuordnung.
- [Flow Control](https://artemis.apache.org/components/artemis/documentation/latest/flow-control.html), [Paging](https://artemis.apache.org/components/artemis/documentation/latest/paging.html): bedarfsgesteuerter Empfang, begrenzter Speicher und PAGE. Producer-Credit-Window ist ausgeschaltet (`-1`); persistente Sends bleiben synchron mit Antwortfrist, die CLI-Last ist klein und begrenzt.
- [Jakarta Messaging 3.1 Spezifikation](https://jakarta.ee/specifications/messaging/3.1/jakarta-messaging-spec-3.1.html): Acknowledgement, Delivery Delay, Shared Durable, Correlation. Delivery Delay wird zusätzlich gegen die tatsächlich gepackte Client-/Brokerkombination getestet.

Diese Quellenprüfung ist kein erfolgreicher Plattformtest. Ausgeführte Abnahme und offene Plattformen stehen in acceptance.md.

Die Wrapper-Properties enthalten SHA-256-Prüfsummen für Maven-Distribution und Bootstrap-JAR. GitHub Actions sind auf die am Prüftag über die offiziellen GitHub-Release-/Tag-APIs aufgelösten Commits festgelegt: checkout v7.0.1 (`3d3c42e5aac5ba805825da76410c181273ba90b1`), setup-java v6.1.0 (`b843ebf9b913146b38da2eaa7aeddf76074ae1ad`), upload-artifact v7.0.2 (`cf430e030ddbb5b0abf93d22962f4752f3646cd9`). Quellen: [checkout](https://github.com/actions/checkout/releases/tag/v7.0.1), [setup-java](https://github.com/actions/setup-java/releases/tag/v6.1.0), [upload-artifact](https://github.com/actions/upload-artifact/releases/tag/v7.0.2).
