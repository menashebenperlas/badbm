package edu.touro.mco152.bm;

/**
 * Defines UI-agnostic operations to control disk benchmarks.
 */
public interface BenchmarkController {
    void startBenchmark();
    void stopBenchmark();

    /**
     * Return the most recent run parameters, or null if never run.
     */
    RunParameters getLastRunParameters();
}