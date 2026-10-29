# Windows ohne Administratorrechte

Ein vom Trainer bereitgestelltes **JDK 21** entpacken oder die vorhandene Installation nutzen. Keine Dienste, Registry-Änderungen, Docker/WSL oder PowerShell-Ausführungsrichtlinien sind nötig. In **cmd.exe** prüfen:

```bat
where java
java -version
javac -version
```

Falls nötig nur im aktuellen Terminal setzen (Pfad anpassen):

```bat
set "JAVA_HOME=C:\Tools\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "C:\Training\Artemis Lab"
.\mvnw.cmd clean verify
.\scripts\doctor.cmd build
```

Ein JRE reicht für den Quellbuild nicht. Der Wrapper liegt mit seiner Bootstrap-JAR im Repository; `mvnw.cmd` benötigt keine Freigabe für PowerShell-Skripte. Die erstmalige Maven-Distribution und Dependencies werden über HTTPS bezogen. Firmenproxy, interne Mirrors und Zertifikate nach Unternehmensvorgaben über vorhandene `%USERPROFILE%\.m2\settings.xml` und den bereitgestellten Java-Truststore konfigurieren. Die Wrapper-Distribution wird vor Maven selbst geladen: dafür muss auch deren HTTPS-URL freigegeben oder der Wrapper-Cache vom Trainer vorbereitet sein. Maven-Proxy-Einstellungen greifen erst nach dem Bootstrap. Keine TLS-Prüfung abschalten.

## Drei getrennte Terminals

Alle Terminals öffnen und jeweils ins Repository wechseln. Terminal 1 startet `scripts\broker.cmd start` und wartet auf `BROKER_READY`. Terminal 2 startet `scripts\lab.cmd queue consume --worker alice --count 10` und wartet auf `READY`. Erst dann führt Terminal 3 `scripts\lab.cmd queue produce --count 10` aus. [README](../README.md) enthält die vollständigen Befehle.

Der Broker läuft absichtlich im Vordergrund. Nicht alle Befehle hintereinander ins blockierte Terminal 1 kopieren. Strg+C beendet ihn geordnet. `scripts\broker.cmd stop` fordert aus einem anderen Terminal ebenfalls einen geordneten Stopp an. Vor Neustart oder Reset auf das Prozessende warten.

## Vorgebautes ZIP

Das vom Trainer gebaute Laufzeit-ZIP entpacken. Es braucht für die Demos nur das passende JDK, keinen Compilerlauf, Maven, Netzwerkzugriff oder IDE. Die Skripte suchen `lib/*.jar`; im Quellrepository verwenden sie alternativ die `target`-JARs. Aufrufe mit absolutem Skriptpfad funktionieren auch außerhalb des Repositoryverzeichnisses und bei Leerzeichen. Eigene relative `--config`-/Store-/Datenpfade werden zum Lab-Hauptverzeichnis aufgelöst.

Ein **Offline-Quellbuild** ist etwas anderes: Wrapper-Distribution, alle Dependencies und Plugins müssen vorher im Maven-Cache liegen. Ein bereits ausgeführtes `clean verify -Ptraining` ist eine gute Vorbereitung; anschließend `mvnw.cmd -o clean verify -Ptraining` prüfen. Der Cache allein ersetzt kein JDK.

## Typische Einstiegsprobleme

`JAVA_HOME` muss auf das JDK-Hauptverzeichnis zeigen, nicht auf `bin`. `where java` kann mehrere Installationen zeigen; die Skripte bevorzugen `JAVA_HOME`. Bei fehlenden Artefakten zunächst bauen. Bei Portkonflikten denselben `--port 61620` in Broker- und allen Clientaufrufen verwenden. Eigene Daten werden unter `runtime/` angelegt. Keine fremden Prozesse oder Daten zur Fehlerbehebung löschen.
