package edu.touro.mco152.bm;

import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.persist.EM;
import edu.touro.mco152.bm.ui.Gui;

import jakarta.persistence.EntityManager;
import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.touro.mco152.bm.App.*;
import static edu.touro.mco152.bm.DiskMark.MarkType.READ;
import static edu.touro.mco152.bm.DiskMark.MarkType.WRITE;

import edu.touro.mco152.bm.BenchmarkService;
import edu.touro.mco152.bm.BenchmarkServiceImpl;

/**
 * Swing‐compliant worker that now also serves as the controller
 * entry point for starting/stopping benchmarks.
 */
public class DiskWorker extends SwingWorker<Boolean, DiskMark>
        implements BenchmarkController {

    private final BenchmarkService service = new BenchmarkServiceImpl();

    private Boolean lastStatus = null;  // record of final status

    // --- BenchmarkController methods ---

    @Override
    public void startBenchmark() {
        // kicks off the service on its own thread
        new Thread(() -> service.runBenchmark()).start();
    }

    @Override
    public void stopBenchmark() {
        // requests cancellation of the SwingWorker
        cancel(true);
        service.stopBenchmark();
    }

    @Override
    public RunParameters getLastRunParameters() {
        // Fetch all persisted runs, take the most recent
        List<DiskRun> runs = DiskRun.findAll();
        if (runs == null || runs.isEmpty()) {
            return null;
        }
        DiskRun last = runs.get(runs.size() - 1);
        return new RunParameters(
                last.getTotalMarks(),
                last.getRunMin(),
                last.getRunMax(),
                last.getRunAvg()
        );
    }

    // Existing SwingWorker logic below

    @Override
    protected Boolean doInBackground() throws Exception {
        // Delegates to the new service
        return service.runBenchmark();
    }

    @Override
    protected void process(List<DiskMark> chunks) {
        for (DiskMark dm : chunks) {
            if (dm.type == WRITE) {
                Gui.addWriteMark(dm);
            } else {
                Gui.addReadMark(dm);
            }
        }
    }

    @Override
    protected void done() {
        try {
            lastStatus = super.get();
        } catch (Exception e) {
            Logger.getLogger(App.class.getName())
                    .log(Level.WARNING, "Problem obtaining final status", e);
        }
        if (App.autoRemoveData) {
            Util.deleteDirectory(dataDir);
        }
        App.state = App.State.IDLE_STATE;
        Gui.mainFrame.adjustSensitivity();
    }

    /** Expose final status if callers wish to inspect it. */
    public Boolean getLastStatus() {
        return lastStatus;
    }
}