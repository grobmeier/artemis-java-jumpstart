# Optionaler zentraler Trainer-Broker

Lokal bleibt der Standard. Dieses Profil startet denselben Java-Broker auf Linux und verwendet dieselben Client-JARs. Hier werden keine Änderungen an einem echten Server vorgenommen. Der Trainer betreibt einen eigenen Trainingsprozess mit eigenem Datenbereich und JDK 21; kein bestehender Produktivbroker wird umkonfiguriert.

## Broker vorbereiten (Trainer, Linux)

Laufzeit-ZIP entpacken, nicht als root betreiben. Der Datenpfad muss innerhalb des eigenen `runtime/` liegen. `config/remote.properties` als `config/remote.local.properties` kopieren. Reale Werte lokal eintragen:

```properties
profile=remote
host=broker.training.example
bind=0.0.0.0
port=61617
namespace=trainer
ssl=true
keyStore=config/server.p12
usersFile=config/users.properties
```

Der Bind ist ausdrücklich nötig. `0.0.0.0` nur auf dem für das Training freigegebenen Server; alternativ konkrete Server-IP verwenden. Firewall, DNS, Zertifikatsname und Freigabe des TLS-TCP-Ports mit der zuständigen IT abstimmen. Dieses Programm stellt keine Webkonsole und keinen Management-HTTP-Port bereit.

TLS-Schlüsselspeicher im PKCS12-Format mit privatem Schlüssel und vollständiger Zertifikatskette vom Trainer/der IT bereitstellen. Das Serverzertifikat braucht den tatsächlich verwendeten DNS-Namen als Subject Alternative Name. Kein `trustAll`, kein Abschalten der Hostname-Prüfung, kein Rückfall auf Klartext. Zertifikatsablauf vor dem Training prüfen. Für eine isolierte lokale Probe kann der Trainer mit dem JDK-Werkzeug `keytool` eine eigene Trainings-CA beziehungsweise ein Testzertifikat erstellen; Clients vertrauen dann ausdrücklich nur dieser Trainingswurzel. Nicht einfach irgendein ungeprüftes Serverzertifikat importieren.

Individuelle Nutzer in der nicht eingecheckten `config/users.properties` (Werte nur als Platzhalter dargestellt):

```properties
alice.password=<INDIVIDUELL_LOKAL_SETZEN>
alice.namespace=alice
bob.password=<ANDERES_PASSWORT_LOKAL_SETZEN>
bob.namespace=bob
# Optional ausschließlich für bewusst gemeinsame Übungen:
# alice.groups=team
# bob.groups=team
```

Dateirechte auf den Trainer beschränken (`chmod 600` für lokale Konfiguration, Nutzerdatei und Schlüsseldatei). Die aktuelle Trainingsimplementierung lädt diese kleine private Nutzerdatei beim Start; sie ist kein produktiver Identity Provider. Keine echten Nutzerdaten verwenden. Die Rechte erlauben Senden, Empfangen, Browsen und Lebenszyklus der Trainingsziele im eigenen autorisierten Address-Bereich. Kein Managementrecht; Verwaltungszugriff bleibt beim Trainer im lokalen Brokerprozess. Persönliche Bereiche und optionale Gruppen werden explizit zugeordnet. Remote liegen auch die Fehleradressen im autorisierten Namespace: `<namespace>.sNN.DLQ` beziehungsweise `.EXPIRY`. Ihre Queues tragen zusätzlich den Quelladdress-Namen. Fremde Fehlerbereiche bleiben unzugänglich.

Passwort des Schlüsselspeichers in einer geschützten lokalen Datei als `keyStorePassword` oder über `LAB_KEY_STORE_PASSWORD` bereitstellen. Keine echten Passwörter als CLI-Argumente oder im Shellverlauf empfehlen. Brokerstart im Vordergrund:

```sh
./scripts/broker.sh start --config config/remote.local.properties
```

Auf `BROKER_READY` warten. In einem weiteren Serverterminal kann derselbe Trainer `./scripts/broker.sh stop --config config/remote.local.properties` aufrufen. Auf das tatsächliche Prozessende warten. Neustart mit derselben Konfiguration und demselben Datenpfad. Der Broker gibt lokal schreibgeschützt Queue-Tiefe, Consumerzahl und aktive Zustellungen aus. Kein automatisch vorhandenes Browser-Management-Interface voraussetzen.

## Client vorbereiten (Firmenrechner)

Trainer verteilt den geprüften Truststore mit der vereinbarten CA und nennt Host, Port, Namespace und individuellen Nutzer. Remote-Konfiguration lokal kopieren:

```properties
profile=remote
host=broker.training.example
port=61617
namespace=alice
ssl=true
user=alice
trustStore=config/training-trust.p12
```

`password` und `trustStorePassword` nur in der privaten `*.local.properties`-Datei mit restriktiven Rechten oder als `LAB_PASSWORD`/`LAB_TRUST_STORE_PASSWORD` setzen. Remote verwendet keine lokalen Demo-Defaults. Eine Remote-Anforderung mit der lokalen Profildatei wird verweigert. Rangfolge bleibt CLI > LAB_-Umgebung > gewählte Datei > sichere Defaults; Secrets über private Datei oder Umgebung, nicht über CLI teilen. Relative Pfade beziehen sich auf das Lab-Verzeichnis.

In cmd.exe vor dem Training:

```bat
scripts\doctor.cmd runtime --config config\remote.local.properties
```

Die Probe prüft **TCP, TLS mit Hostname-/Zertifikatsprüfung und Authentifizierung** mit kurzer Frist. Anschließend eine Nachricht im eigenen Bereich senden und empfangen, um auch Berechtigungen zu prüfen:

```bat
scripts\lab.cmd s01 produce --count 1 --config config\remote.local.properties
scripts\lab.cmd s01 consume --count 1 --config config\remote.local.properties
```

Die Testsuite erzeugt eigene kurzlebige Zertifikate und prüft dieses Profil lokal, einschließlich falschem Hostnamen, fremdem Namespace, geteiltem Gruppenbereich und fremder DLQ. Das ersetzt keine Verbindungsprobe vom tatsächlichen Firmenrechner zum Trainer-Server.

## Gemeinsame Übung und Grenzen

Für A/S02 brauchen beide dieselbe autorisierte Gruppenqueue: beide Nutzer erhalten vom Trainer `groups=team`, dann bei beiden `--namespace team`. Für B/S03 nutzen sie denselben autorisierten Topic-Bereich und getrennte aktive Subscriptions. Für persönliche Versuche bleiben sie in alice/bob. E/H2 speichert auf jedem Rechner lokal und liefert keine rechnerübergreifende Idempotenzgarantie.

HTTPS-Erreichbarkeit einer Website beweist keine Broker-Erreichbarkeit. Ein anderer Port umgeht keinen verpflichtenden HTTP-Proxy; das CORE-TCP-Protokoll ist kein HTTP. Bei Firewall-/Proxy-Sperren den lokalen Broker beziehungsweise das vorgebaute ZIP verwenden. Unternehmensvorgaben nicht umgehen.

Remote nie den gemeinsamen Broker zurücksetzen, um eine einzelne Übung zu säubern. Eigene Nachrichten kontrolliert konsumieren, eigene durable Identität mit unsubscribe entfernen; leere Queue gezielt mit cleanup --confirm entfernen. Nur der Trainer darf nach Ende **aller** Nutzer einen vollständigen Reset seines eigenen Server-Datenbereichs durchführen. Jede Instanz braucht ein eigenes Laufzeitverzeichnis; die Dateisperre erlaubt absichtlich nur einen Broker pro Lab-Verzeichnis.
