# Windows without administrator rights

Use an existing **JDK 21** or extract the trainer's supplied JDK. No service installation, registry changes, Docker/WSL or PowerShell policy change is needed. In **cmd.exe**:

```bat
where java
java -version
javac -version
```

If needed, set only the current terminal environment (adjust paths):

```bat
set "JAVA_HOME=C:\Tools\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "C:\Training\Artemis Lab"
.\scripts\doctor.cmd build
```

Only after `BUILD_READY`, build separately:

```bat
.\mvnw.cmd -Ptraining clean verify
```

A JRE cannot compile source. The wrapper bootstrap JAR is included; mvnw.cmd needs no PowerShell approval. First use downloads the Maven distribution and dependencies over HTTPS. Configure company proxy/mirrors/certificates through the approved `%USERPROFILE%\.m2\settings.xml` and supplied Java truststore. Wrapper download happens before Maven: its HTTPS URL also needs access or a prepared wrapper cache. Maven proxy settings apply after bootstrap. Keep TLS verification enabled.

## Separate terminals

Windows Terminal and VS Code often open PowerShell by default. The `set` and `cd /d` commands above are **cmd.exe** syntax. Type `cmd` to use those examples, or set the current PowerShell environment this way before invoking the same `.cmd` scripts:

```powershell
$env:JAVA_HOME = 'C:\Tools\jdk-21'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Set-Location 'C:\Training\Artemis Lab'
.\scripts\doctor.cmd build
```

Run the environment setup in **each new terminal**. `JAVA_HOME` is required for the Windows Maven Wrapper even when `java` is on PATH. `doctor build` needs no built JAR or broker. Stop here if it does not report `BUILD_READY`; fix the JDK/source download before building. Wait for `BUILD SUCCESS` before starting the broker.


Open each terminal in the lab root. Terminal 1 runs `scripts\broker.cmd start`; wait for `BROKER_READY`. Terminal 2 starts a worker; wait for `READY` before Terminal 3 publishes. [README](../README.md) contains the full sequence. Competing workers need a fourth terminal. Broker start deliberately blocks; do not paste client commands into its terminal.

Ctrl+C or `scripts\broker.cmd stop` from another terminal requests graceful broker stop. Wait for actual exit before restart/reset. Java output uses UTF-8. The cmd launcher temporarily changes console code page for Java and restores it on normal exit; no registry/policy changes occur.

## Prebuilt ZIP and offline source builds

Extract the verified runtime ZIP. Demos need the matching JDK, no compile step, Maven, network or IDE. Scripts look in lib/*.jar, or module target JARs in the source repository. Absolute script invocation works from other directories and paths with spaces. Relative configuration/store/data paths resolve against the lab root.

Editing tasks still requires source and a build environment. An **offline source build** needs the wrapper distribution, all dependencies and plugins prepared in cache. Run `mvnw.cmd -Ptraining clean verify` beforehand, then test `mvnw.cmd -o -Ptraining clean verify`. A cache does not replace a JDK.

JAVA_HOME points to the JDK root, not bin. `where java` can list several versions; scripts prefer JAVA_HOME. Build missing artifacts first. For a port conflict, use the same alternate `--port 61620` in broker and every client. Data belongs under runtime/. Do not delete unrelated processes or data.
