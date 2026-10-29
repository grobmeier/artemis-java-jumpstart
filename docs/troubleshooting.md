# Fehler finden

| Beobachtung | Prüfung und Maßnahme |
|---|---|
| Java fehlt / falsche Version | `where java` beziehungsweise `command -v java`, `java -version`, JAVA_HOME prüfen; JDK 21 nutzen |
| Build-Artefakt fehlt | aus dem Quellrepository `mvnw.cmd clean verify`; Übung/Lösung zusätzlich `-Ptraining`; im Laufzeitpaket lib prüfen |
| Wrapper kann Maven nicht laden | Firmenfreigabe, Bootstrap-Cache, HTTPS und Java-Truststore prüfen; danach Maven settings.xml für Artefakte |
| Port ist belegt | eigenen Broker im anderen Terminal suchen; bei anderem Dienst alternative Portnummer in allen Prozessen wählen; fremden Prozess nicht blind beenden |
| doctor meldet Broker-Port nicht erreichbar | Broker separat starten, BROKER_READY abwarten, Host/Port/Firewall prüfen |
| Authentifizierung fehlgeschlagen | richtige Profildatei, individuellen Nutzer und privates Passwort prüfen; keine Geheimnisse in Logs kopieren |
| Remote-TLS scheitert | Truststore, Kette, Ablaufdatum, PKCS12-Passwort und SAN passend zum DNS-Namen prüfen; Verifikation eingeschaltet lassen |
| Consumer TIMEOUT | READY vor Publish? gleicher Namespace/Szenario? korrektes count? Topic-Historie existiert bei neuen nicht dauerhaften Subscriptions nicht |
| Durable Subscriber bekommt nichts | dieselbe Client-ID UND denselben Subscription-Namen wie bei der Registrierung verwenden; erst registrieren, dann offline senden |
| Consumer erhält erneut | vorheriger Commit fehlt, Rollback oder Crash; fachliche ID und Redelivery-Felder vergleichen |
| S06 läuft länger als erwartet | drei Versuche für order-2 plus gesunde Nachrichten; bei count=3 Produce sind fünf Receives zu erwarten |
| Idempotenz zeigt mehr als einen Eintrag | frischen Namespace verwenden, gleiche fachliche ID für beide Sends, H2-Dateipfad gleich lassen; WRITE_DELAY=0 und atomaren INSERT/Commit prüfen |
| H2-Datei belegt | S09 hat einen lokalen Worker pro Namespace/Ledger; anderen Worker beenden, keine Live-Datei löschen |
| Reset verweigert | Broker wirklich beendet? eigener Marker? Daten unter runtime? Symlink/Junction? Schutz prüfen, nicht umgehen |
| Cleanup verweigert | Queue muss leer und ohne aktive Consumer sein; erst bekannte Restmenge bearbeiten, dann Worker stoppen |
| wenig gleichmäßige Verteilung | keine feste Quote garantiert; zuerst beide READY, genügend Nachrichten und kleine Bearbeitungszeit; consumerWindowSize beeinflusst Vorabzuordnung |
| Send schlägt nach Frist fehl | Broker/Plattenplatz prüfen; synchroner Send kann nach Timeout einen unklaren Ausgang haben, beim Wiederholen dieselbe Fach-ID verwenden |

`doctor build` prüft JDK/Wrapper, nicht die Erreichbarkeit von Maven-Repositories. `doctor runtime` prüft JARs, TCP und authentifizierte Verbindung, nicht jede Zielberechtigung. Brokerstart ist blockierend und beabsichtigt. `--count` muss positiv sein; für Dauerbetrieb ausdrücklich `--continuous` verwenden. Unerwartete Fehler ergeben einen kompakten deutschen Hinweis statt einer Passwort enthaltenden Provider-Stacktrace-Ausgabe.

Ein begrenzter Consumer endet bei fehlender Sollmenge mit Exitcode 2. Bei einem manuellen Verteilungsversuch kann ein Worker deshalb nicht einfach count=5 garantieren; verwendet Dauerbetrieb und vergleicht die Gesamtmenge. Der absichtliche Crash endet mit 17. Nach einem Netzabbruch darf die Brokererkennung einige Sekunden brauchen; Tests und Übungen verwenden begrenzte Wartefristen.

Für gezieltes Entfernen einer leeren eigenen Queue: `scripts\lab.cmd s01 cleanup --confirm`. Bei Bedarf `--target outgoing`, `--target dlq` oder `--target expiry`; Namespace/Szenario bleiben begrenzt. Durable Topic-Ressourcen mit `unsubscribe --subscription <NAME>` und derselben Identität entfernen. Kein Cleanup entfernt fremde Bereiche oder ein laufendes Ziel. Ganze lokale Brokerdaten nur nach geordnetem Stopp mit `broker reset --confirm`; H2-Ledger separat.

Bei einem verschobenen **bereits benutzten** Laufzeitpaket passt der Datenmarker absichtlich nicht mehr zum kanonischen Pfad. Für ein neues Training ein frisches ZIP entpacken. Vorhandene Daten nicht durch Ändern des Markers „reparieren“: Trainer entscheidet über kontrollierte Übernahme beziehungsweise gesicherte Neuanlage. Unbenutzte ZIPs sind frei verschiebbar.
