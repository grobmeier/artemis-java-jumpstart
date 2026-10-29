# Gemeinsam im privaten GitHub-Repository arbeiten

Repository: [grobmeier/artemis-java-jumpstart](https://github.com/grobmeier/artemis-java-jumpstart). Der Quellcode, die Übungen und die getrennten Musterlösungen liegen zusammen darin. Sichtbarkeit: **privat**. Ein Link allein gibt noch keinen Zugriff.

## Trainer: Personen freischalten

1. GitHub-Benutzernamen der Trainees erfragen; jede Person verwendet ihr eigenes Konto.
2. [Settings → Collaborators](https://github.com/grobmeier/artemis-java-jumpstart/settings/access) öffnen und **Add people** wählen.
3. Die konkreten Personen einladen. Erst nach Annahme der Einladung können sie auf das private Repository zugreifen.
4. Für jeden Trainee einen eigenen Arbeitszweig vereinbaren; Änderungen über Pull Requests besprechen.

Bei privaten Repositories eines persönlichen GitHub-Kontos erhalten Collaborators Lese- und Schreibzugriff. Es gibt dort keine getrennte Nur-Lesen-Collaboratorrolle. Sie können deshalb auch die Musterlösungen und die Arbeitszweige der anderen sehen. Dieses Repository dient dem gemeinsamen Training; voneinander abgeschottete Prüfungsabgaben würden einen anderen Aufbau benötigen. [Offizielle Zugriffsrechte](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/repository-access-and-collaboration/permission-levels-for-a-personal-account-repository), [Einladungen](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/repository-access-and-collaboration/inviting-collaborators-to-a-personal-repository).

Bis konkrete GitHub-Benutzernamen benannt sind, werden keine Einladungen versendet. Zugangsdaten oder SSH-Schlüssel des Trainers werden nicht weitergegeben.

## Trainee: starten und eigene Änderungen sichern

Nach Annahme der Einladung in cmd.exe:

```bat
git clone https://github.com/grobmeier/artemis-java-jumpstart.git
cd artemis-java-jumpstart
git switch -c feature/YOUR_GITHUB_NAME-exercise-a
.\mvnw.cmd -Ptraining clean verify
```

`YOUR_GITHUB_NAME` im Zweignamen ersetzen. Anschließend [Aufgaben A–F](exercises.md) bearbeiten, beispielsweise in `exercises/src/main/java/de/jumpstart/AExercise.java`. Broker und Clients wie dort beschrieben in getrennten Terminals starten. Die Testprüfung erwartet ausdrücklich nicht, dass Übungsstellen offen bleiben: eigene Implementierungen sind erlaubt. Die vollständige Suite prüft die fertigen Demos und Musterlösungen weiterhin.

Für kleine abgeschlossene Änderungen:

```bat
git add exercises/src/main/java/de/jumpstart/AExercise.java
git commit -m "Implement exercise A queue producer and worker"
git push -u origin feature/YOUR_GITHUB_NAME-exercise-a
```

Danach auf GitHub einen Pull Request gegen `main` öffnen. Der Trainer und die Gruppe können den Code darin besprechen. Für die nächste Aufgabe denselben persönlichen Zweig fortsetzen oder nach Absprache einen weiteren anlegen. Andere Arbeitszweige und `main` nicht überschreiben. Eigene Tests für den bearbeiteten Fachablauf sind willkommen; die Referenztests sind keine automatische Bewertung jeder individuellen Lösung.

Bei Bedarf den aktuellen gemeinsamen Stand holen: zuerst eigene Änderungen committen, dann `git fetch origin` und im eigenen Zweig `git merge origin/main`. Bei Konflikten gemeinsam mit dem Trainer auflösen. Kein `reset --hard` oder Force-Push zur routinemäßigen Fehlerbehebung.

Ohne installiertes Git ist nach Anmeldung **Code → Download ZIP** für lokale Übungen möglich. Um Änderungen auf GitHub zu teilen, braucht ihr Git oder die von eurem Unternehmen bereitgestellte IDE-Integration. Die [Windows-Einrichtung](windows-setup.md) benötigt keine Administratorrechte für das Lab selbst.

## Was bedeutet die CI?

**CI** steht für Continuous Integration: GitHub führt nach Änderungen automatisch denselben Build und dieselben Tests aus. Hier übernimmt das **GitHub Actions**. Es gibt keine zusätzliche CI-Installation für Trainees.

Der Workflow **Verify Java lab** läuft nach Pushes, bei Pull Requests und auf manuellen Start. Zwei getrennte GitHub-Rechner prüfen mit JDK 21:

| Prüfung | Was sie macht |
|---|---|
| Verify (ubuntu-latest) | Quellcode bauen, alle Messaging- und Prozessprüfungen unter Linux ausführen |
| Verify (windows-latest) | Dieselben Prüfungen unter Windows, einschließlich cmd-Launcher und Junction-Schutz |

Der Build aktiviert `-Ptraining`, damit Übungsgerüste und Musterlösungen mitkompilieren. Tests starten eigene lokale Broker und beenden sie danach. Weder ein Trainer-Server noch echte Zahlungs-, E-Mail- oder Versanddienste werden angesprochen. Für den Workflow werden keine geheimen Broker-Zugangsdaten benötigt; er arbeitet mit synthetischen Testprofilen.

Ein grüner Haken bedeutet, dass die Prüfungen für genau diesen Commit bestanden sind. Bei rotem Status den fehlgeschlagenen Lauf öffnen und die betroffene Prüfung/Testausgabe lesen; nach Korrektur erneut pushen. Der tatsächliche Plattformstatus und belegte Ergebnisse stehen in [acceptance.md](acceptance.md).

## Vorgebautes Paket und Testberichte herunterladen

1. [Actions](https://github.com/grobmeier/artemis-java-jumpstart/actions) öffnen.
2. Einen **erfolgreichen** Lauf des gewünschten Commits auswählen.
3. Unter **Artifacts** `lab-windows-latest` oder `lab-ubuntu-latest` herunterladen (Anmeldung und Repositoryzugriff erforderlich).
4. Das GitHub-Artefakt entpacken; darin liegt `artemis-java-jumpstart-runtime.zip` neben den Testberichten. Auch dieses Laufzeit-ZIP entpacken und **RUN.md** lesen.

Das Paket braucht lokal nur das passende JDK. Die Quellübungen sind im geklonten Repository; sie sind nicht Bestandteil des kleinen Laufzeitpakets. Artefakte werden sieben Tage aufbewahrt. Der Trainer sollte das geprüfte Paket vor dem Training lokal bereitlegen. Ein manueller neuer Lauf ist über **Verify Java lab → Run workflow** möglich.
