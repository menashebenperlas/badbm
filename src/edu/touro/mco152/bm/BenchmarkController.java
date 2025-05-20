package edu.touro.mco152.bm;

/**
 * Defines UI-agnostic operations to control disk benchmarks.
 */
public interface BenchmarkController {
    void startBenchmark();
    void stopBenchmark();
    RunParameters getLastRunParameters();
}