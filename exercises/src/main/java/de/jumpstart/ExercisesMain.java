package de.jumpstart;

public final class ExercisesMain {
    private ExercisesMain() {}

    public static void main(String[] args) {
        Output.configure();
        try {
            var options = new Options(args);
            var settings = new Settings(options);
            switch (options.word(0, "A").toUpperCase(java.util.Locale.ROOT)) {
                case "A" -> AExercise.run(settings, options);
                case "B" -> BExercise.run(settings, options);
                case "C" -> CExercise.run(settings, options);
                case "D" -> DExercise.run(settings, options);
                case "E" -> EExercise.run(settings, options);
                case "F" -> FExercise.run(settings, options);
                default -> throw new UsageException("Aufgabe A bis F wählen.");
            }
        } catch (Exception e) {
            Errors.report(e);
            System.exit(2);
        }
    }
}
