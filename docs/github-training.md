# Work together in the public GitHub repository

[grobmeier/artemis-java-jumpstart](https://github.com/grobmeier/artemis-java-jumpstart) contains public source, exercises and separate worked solutions. Anyone can read, clone or download the source without an invitation. A GitHub account is needed to fork the repository and submit a pull request.

## Practise locally or contribute through a fork

For local practice, follow the clone and build instructions in [README](../README.md). You can create a local branch without write access to the original repository.

To share your work, sign in to your own GitHub account and choose **Fork** on the repository page. Replace YOUR_GITHUB_NAME in the commands below with your account name. Clone your fork, add the original repository as upstream and create a personal branch:

```bat
git clone https://github.com/YOUR_GITHUB_NAME/artemis-java-jumpstart.git
cd artemis-java-jumpstart
git remote add upstream https://github.com/grobmeier/artemis-java-jumpstart.git
git switch -c feature/YOUR_GITHUB_NAME-exercise-a
```

Set JAVA_HOME to JDK 21 in each terminal, then run the preflight separately:

```bat
.\scripts\doctor.cmd build
```

Only after BUILD_READY, compile and wait for BUILD SUCCESS:

```bat
.\mvnw.cmd -Ptraining clean verify
```

Edit [tasks A–F](exercises.md), for example exercises/src/main/java/de/jumpstart/AExercise.java. Start broker/clients in separate terminals. Tests allow completed trainee scaffolds and still verify independent reference demos/solutions; green CI does not automatically grade every trainee implementation.

Save small completed changes and push to your own fork:

```bat
git add exercises/src/main/java/de/jumpstart/AExercise.java
git commit -m "Implement exercise A queue producer and worker"
git push -u origin feature/YOUR_GITHUB_NAME-exercise-a
```

Open a pull request from your fork to the original repository's main branch. To update your work, commit it, run git fetch upstream, then git merge upstream/main on your branch; resolve conflicts with the trainer. Do not routinely use reset --hard or force push. Direct pushes to the original repository still require write access; making the repository public does not grant it. Never share trainer credentials or SSH keys.

Without Git, **Code → Download ZIP** permits local practice. Sharing changes requires Git or an approved IDE integration. The lab needs no administrator rights.

## Shared protected baseline

`main` is protected. Contributions use pull requests with the Windows, Linux and macOS verification checks passing against the current base. Force pushes/deletion of main are blocked. The owner retains the administrative maintenance exception.

## What is CI?

**Continuous integration (CI)** runs the build/tests automatically after changes. Here it is **GitHub Actions**; trainees install no separate CI system. The **Verify Java lab** workflow runs on pushes, pull requests and manual dispatch with JDK 21:

| Check | Behavior |
|---|---|
| Verify (ubuntu-latest) | Build source and run real messaging/process checks on Linux |
| Verify (windows-latest) | Same checks on Windows, including cmd launchers and junction guards |
| Verify (macos-latest) | Same real-process and shell-launcher checks on macOS |

The training profile compiles scaffolds and solutions. Tests create and stop local brokers, using synthetic profiles and temporary certificates. They do not use a trainer server, real payment/email/shipping services or secret broker credentials. A green check applies to that exact commit. Open failed run logs, fix the issue and push again. See [acceptance evidence](acceptance.md).

## Download a verified runtime and reports

1. Open [Actions](https://github.com/grobmeier/artemis-java-jumpstart/actions).
2. Select a successful run of the agreed revision.
3. Download **lab-windows-latest**, **lab-ubuntu-latest** or **lab-macos-latest** under Artifacts (GitHub sign-in required).
4. Extract the artifact, then its artemis-java-jumpstart-runtime.zip, and read RUN.md.

The ZIP runs with the supplied JDK. Editable tasks remain in the source repository. Artifacts expire after seven days: prepare the package locally before training. **Verify Java lab → Run workflow** permits a new manual run.
