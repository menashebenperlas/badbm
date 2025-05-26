package edu.touro.mco152.bm;

public class BenchmarkCLI {
    public static void main(String[] args) throws InterruptedException {

        BenchmarkController launcher = new BenchmarkLauncher();

        System.out.println("Starting benchmark...");
        launcher.startBenchmark();

        // waits for results
        RunParameters params;
        while ((params = launcher.getLastRunParameters()) == null) {
            Thread.sleep(250);
        }

        System.out.println("Done!  Results: " + params);
        // cleans up threads
        if (launcher instanceof BenchmarkLauncher) {
            ((BenchmarkLauncher) launcher).shutdown();
        }
    }
}