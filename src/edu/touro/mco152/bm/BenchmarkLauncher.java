package edu.touro.mco152.bm;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * A reusable, thread-safe entrypoint for starting/stopping benchmarks
 * via the BenchmarkController interface. Delegates to the service layer.
 */
public class BenchmarkLauncher implements BenchmarkController {
    // single-threaded executor for benchmarks
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final BenchmarkService service = new BenchmarkServiceImpl();

    @Override
    public void startBenchmark() {
        // submit the service.runBenchmark() task asynchronously
        exec.submit(() -> {
            boolean success = service.runBenchmark();
        });
    }

    @Override
    public void stopBenchmark() {
        // ask the service to stop
        service.stopBenchmark();
    }

    @Override
    public RunParameters getLastRunParameters() {
        // reuse service to fetch all runs, then take the last one
        var all = service.getAllRuns();
        return all.isEmpty() ? null : all.get(all.size() - 1);
    }

    /**
     * Clean up when application is shutting down.
     */
    public void shutdown() {
        exec.shutdownNow();
    }
}