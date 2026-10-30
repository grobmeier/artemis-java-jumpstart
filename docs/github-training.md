# Work together in the private GitHub repository

[grobmeier/artemis-java-jumpstart](https://github.com/grobmeier/artemis-java-jumpstart) contains source, exercises and separate worked solutions. It remains **private**. A link alone grants no access.

## Trainer: invite named people

1. Obtain each trainee's GitHub username; each person uses their own account.
2. Open [Settings → Collaborators](https://github.com/grobmeier/artemis-java-jumpstart/settings/access) and choose **Add people**.
3. Invite the specific people; they receive access after accepting.
4. Agree personal branches and discuss changes through pull requests.

Collaborators on this personal-account private repository can read and write, including solutions and other branches. There is no separate read-only collaborator role here. This is shared training, not isolated exam submission. See [GitHub permissions](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/repository-access-and-collaboration/permission-levels-for-a-personal-account-repository) and [invitations](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/repository-access-and-collaboration/inviting-collaborators-to-a-personal-repository). Do not share trainer credentials or SSH keys. Invitations require the intended usernames; none are inferred from the public site.

## Shared protected baseline

`main` is protected. Trainee changes use pull requests with **Verify (ubuntu-latest)** and **Verify (windows-latest)** passing against the current base. Force pushes/deletion of main are blocked. The owner retains the administrative maintenance exception.

After accepting the invitation, in cmd.exe:

```bat
git clone https://github.com/grobmeier/artemis-java-jumpstart.git
cd artemis-java-jumpstart
git switch -c feature/YOUR_GITHUB_NAME-exercise-a
.\mvnw.cmd -Ptraining clean verify
```

Set JAVA_HOME to JDK 21 in each terminal first, following [README](../README.md). Run scripts/doctor build before compiling; it must report BUILD_READY. Wait for BUILD SUCCESS before starting scripts. If cloning fails, verify that your own GitHub account accepted the invitation and that your approved Git tool is signed in. Never use the trainer’s account or credentials.

Replace YOUR_GITHUB_NAME. Edit [tasks A–F](exercises.md), for example exercises/src/main/java/de/jumpstart/AExercise.java. Start broker/clients in separate terminals. Tests allow completed trainee scaffolds and still verify independent reference demos/solutions; green CI does not automatically grade every trainee implementation.

Save small completed changes:

```bat
git add exercises/src/main/java/de/jumpstart/AExercise.java
git commit -m "Implement exercise A queue producer and worker"
git push -u origin feature/YOUR_GITHUB_NAME-exercise-a
```

Open a pull request to main for discussion. Continue your branch or agree another for the next task; do not overwrite others' branches. Add tests for your own behavior where useful. To update: commit your work, git fetch origin, then git merge origin/main on your branch; resolve conflicts with the trainer. Do not routinely use reset --hard or force push.

Without Git, **Code → Download ZIP** permits local practice after sign-in. Sharing changes needs Git or a company-provided IDE integration. The lab itself needs no administrator rights.

## What is CI?

**Continuous integration (CI)** runs the build/tests automatically after changes. Here it is **GitHub Actions**; trainees install no separate CI system. The **Verify Java lab** workflow runs on pushes, pull requests and manual dispatch with JDK 21:

| Check | Behavior |
|---|---|
| Verify (ubuntu-latest) | Build source and run real messaging/process checks on Linux |
| Verify (windows-latest) | Same checks on Windows, including cmd launchers and junction guards |

The training profile compiles scaffolds and solutions. Tests create and stop local brokers, using synthetic profiles and temporary certificates. They do not use a trainer server, real payment/email/shipping services or secret broker credentials. A green check applies to that exact commit. Open failed run logs, fix the issue and push again. See [acceptance evidence](acceptance.md).

## Download a verified runtime and reports

1. Open [Actions](https://github.com/grobmeier/artemis-java-jumpstart/actions).
2. Select a successful run of the agreed revision.
3. Download **lab-windows-latest** or **lab-ubuntu-latest** under Artifacts (sign-in and repository access required).
4. Extract the artifact, then its artemis-java-jumpstart-runtime.zip, and read RUN.md.

The ZIP runs with the supplied JDK. Editable tasks remain in the source repository. Artifacts expire after seven days: prepare the package locally before training. **Verify Java lab → Run workflow** permits a new manual run.
