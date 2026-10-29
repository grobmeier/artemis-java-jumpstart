package de.jumpstart;

public final class SolutionsMain {
    private SolutionsMain() {}

    public static void main(String[] args) {
        Output.configure();
        try {
            var options = new Options(args);
            var settings = new Settings(options);
            switch (options.word(0, "A").toUpperCase(java.util.Locale.ROOT)) {
                case "A" -> ASolution.run(settings, options);
                case "B" -> BSolution.run(settings, options);
                case "C" -> CSolution.run(settings, options);
                case "D" -> DSolution.run(settings, options);
                case "E" -> ESolution.run(settings, options);
                case "F" -> FSolution.run(settings, options);
                default -> throw new UsageException("Aufgabe A bis F wählen.");
            }
        } catch (Exception e) {
            Errors.report(e);
            System.exit(2);
        }
    }
}
