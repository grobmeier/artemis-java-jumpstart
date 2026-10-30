import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.ToolProvider;

/** Source-file launcher: diagnose an unbuilt checkout without Maven or a lab JAR. */
class BuildDoctor {
    public static void main(String[] args) {
        try {
            Path home = Path.of(System.getProperty("lab.home")).toRealPath();
            if (ToolProvider.getSystemJavaCompiler() == null)
                throw new IllegalStateException("A full JDK 21 is required, not a JRE.");
            if (!Files.isRegularFile(home.resolve("pom.xml")))
                throw new IllegalStateException(
                        "This is not the source checkout. The runtime ZIP runs demos only; "
                                + "follow RUN.md or obtain the private source to build exercises.");
            if (!Files.isRegularFile(home.resolve(".mvn/wrapper/maven-wrapper.jar"))
                    || !Files.isRegularFile(home.resolve(".mvn/wrapper/maven-wrapper.properties")))
                throw new IllegalStateException(
                        "Maven Wrapper is missing. Extract the complete source, including .mvn.");
            System.out.println("Java=" + Runtime.version() + " java.home=" + System.getProperty("java.home"));
            if (System.getenv("JAVA_HOME") == null || System.getenv("JAVA_HOME").isBlank())
                throw new IllegalStateException(
                        "Set JAVA_HOME to the JDK directory shown above in this terminal before "
                                + "building. See docs/windows-setup.md or README.md for shell commands.");
            System.out.println(
                    "BUILD_READY: JDK and Maven Wrapper are available. No broker or built JAR is needed. "
                            + "Initial Maven/dependency downloads still require network access or a prepared cache.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(2);
        }
    }
}
