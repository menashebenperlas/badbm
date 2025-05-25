package edu.touro.mco152.bm;

import java.util.List;

/**
 * A service API for running, stopping, and querying benchmarks.
 */
public interface BenchmarkService {
    /** Kick off a benchmark; returns true if it completes successfully. */
    boolean runBenchmark();

    /** Requests cancellation of an in-progress benchmark. */
    void stopBenchmark();

    /** Retrieve the parameters/results of all past runs. */
    List<RunParameters> getAllRuns();
}