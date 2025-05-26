package edu.touro.mco152.bm;

import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.persist.EM;
import edu.touro.mco152.bm.ui.Gui;

import javax.persistence.EntityManager;
import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static edu.touro.mco152.bm.App.*;
import static edu.touro.mco152.bm.DiskMark.MarkType.READ;
import static edu.touro.mco152.bm.DiskMark.MarkType.WRITE;

/**
 * Default implementation of BenchmarkService.
 * Contains the core logic you had in DiskWorker.doInBackground().
 */
public class BenchmarkServiceImpl implements BenchmarkService {

    private final EntityManager em = EM.getEntityManager();

    @Override
    public boolean runBenchmark() {
        Logger.getLogger(App.class.getName())
                .log(Level.INFO, "*** New worker thread started ***");
        msg("Running readTest " + App.readTest + "   writeTest " + App.writeTest);
        msg("num files: " + App.numOfMarks + ", num blks: " + App.numOfBlocks
                + ", blk size (kb): " + App.blockSizeKb + ", blockSequence: " + App.blockSequence);

        int wUnitsComplete = 0, rUnitsComplete = 0;
        int wUnitsTotal = App.writeTest ? numOfBlocks * numOfMarks : 0;
        int rUnitsTotal = App.readTest  ? numOfBlocks * numOfMarks : 0;
        int unitsTotal  = wUnitsTotal + rUnitsTotal;

        int blockSize = blockSizeKb * KILOBYTE;
        byte[] blockArr = new byte[blockSize];
        for (int i = 0; i < blockArr.length; i++) {
            if (i % 2 == 0) blockArr[i] = (byte) 0xFF;
        }

        Gui.updateLegend();
        if (App.autoReset) {
            App.resetTestData();
            Gui.resetTestData();
        }

        int startFileNum = App.nextMarkNumber;

        // WRITE benchmark
        if (App.writeTest) {
            DiskRun run = new DiskRun(DiskRun.IOMode.WRITE, App.blockSequence);
            run.setNumMarks(App.numOfMarks);
            run.setNumBlocks(App.numOfBlocks);
            run.setBlockSize(App.blockSizeKb);
            run.setTxSize(App.targetTxSizeKb());
            run.setDiskInfo(Util.getDiskInfo(dataDir));
            msg("disk info: (" + run.getDiskInfo() + ")");
            Gui.chartPanel.getChart().getTitle().setVisible(true);
            Gui.chartPanel.getChart().getTitle().setText(run.getDiskInfo());

            for (int m = startFileNum; m < startFileNum + App.numOfMarks && !isCancelled(); m++) {
                if (App.multiFile) {
                    testFile = new File(dataDir, "testdata" + m + ".jdm");
                } else {
                    testFile = new File(dataDir, "testdata.jdm");
                }
                DiskMark wMark = new DiskMark(WRITE);
                wMark.setMarkNum(m);
                long startTime = System.nanoTime();
                long bytesWritten = 0;
                String mode = App.writeSyncEnable ? "rwd" : "rw";

                try (RandomAccessFile raf = new RandomAccessFile(testFile, mode)) {
                    for (int b = 0; b < numOfBlocks; b++) {
                        if (App.blockSequence == DiskRun.BlockSequence.RANDOM) {
                            int rLoc = Util.randInt(0, numOfBlocks - 1);
                            raf.seek((long) rLoc * blockSize);
                        } else {
                            raf.seek((long) b * blockSize);
                        }
                        raf.write(blockArr);
                        bytesWritten += blockSize;
                        wUnitsComplete++;
                        setProgress((int) ((float)(rUnitsComplete + wUnitsComplete) / unitsTotal * 100f));
                    }
                } catch (IOException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }

                long elapsedNs = System.nanoTime() - startTime;
                double sec = elapsedNs / 1_000_000_000.0;
                double mb = bytesWritten / (double) MEGABYTE;
                wMark.setBwMbSec(mb / sec);
                msg(String.format("m:%d write IO = %.2f MB/s", m, wMark.getBwMbSec()));
                App.updateMetrics(wMark);
                publish(wMark);

                run.setRunMax(wMark.getCumMax());
                run.setRunMin(wMark.getCumMin());
                run.setRunAvg(wMark.getCumAvg());
                run.setEndTime(new Date());
            }

            EntityManager em = EM.getEntityManager();
            em.getTransaction().begin();
            em.persist(run);
            em.getTransaction().commit();
            Gui.runPanel.addRun(run);
        }

        // Prompt to clear cache between W/R (Swing dialog)
        if (App.readTest && App.writeTest && !isCancelled()) {
            JOptionPane.showMessageDialog(Gui.mainFrame,
                    """
                    For valid READ measurements please clear the disk cache...
                    """,
                    "Clear Disk Cache Now", JOptionPane.PLAIN_MESSAGE);
        }

        // READ benchmark
        if (App.readTest) {
            DiskRun run = new DiskRun(DiskRun.IOMode.READ, App.blockSequence);
            run.setNumMarks(App.numOfMarks);
            run.setNumBlocks(App.numOfBlocks);
            run.setBlockSize(App.blockSizeKb);
            run.setTxSize(App.targetTxSizeKb());
            run.setDiskInfo(Util.getDiskInfo(dataDir));
            msg("disk info: (" + run.getDiskInfo() + ")");
            Gui.chartPanel.getChart().getTitle().setText(run.getDiskInfo());

            for (int m = startFileNum; m < startFileNum + App.numOfMarks && !isCancelled(); m++) {
                if (App.multiFile) {
                    testFile = new File(dataDir, "testdata" + m + ".jdm");
                }
                DiskMark rMark = new DiskMark(READ);
                rMark.setMarkNum(m);
                long startTime = System.nanoTime();
                long bytesRead = 0;

                try (RandomAccessFile raf = new RandomAccessFile(testFile, "r")) {
                    for (int b = 0; b < numOfBlocks; b++) {
                        if (App.blockSequence == DiskRun.BlockSequence.RANDOM) {
                            int rLoc = Util.randInt(0, numOfBlocks - 1);
                            raf.seek((long) rLoc * blockSize);
                        } else {
                            raf.seek((long) b * blockSize);
                        }
                        raf.readFully(blockArr);
                        bytesRead += blockSize;
                        rUnitsComplete++;
                        setProgress((int) ((float)(rUnitsComplete + wUnitsComplete) / unitsTotal * 100f));
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                    String emsg = "Unable to READ: " + ex.getMessage();
                    JOptionPane.showMessageDialog(Gui.mainFrame, emsg, "Read Error", JOptionPane.ERROR_MESSAGE);
                    msg(emsg);
                    return false;
                }

                long elapsedNs = System.nanoTime() - startTime;
                double sec = elapsedNs / 1_000_000_000.0;
                double mb = bytesRead / (double) MEGABYTE;
                rMark.setBwMbSec(mb / sec);
                msg(String.format("m:%d read IO = %.2f MB/s", m, rMark.getBwMbSec()));
                App.updateMetrics(rMark);
                publish(rMark);

                run.setRunMax(rMark.getCumMax());
                run.setRunMin(rMark.getCumMin());
                run.setRunAvg(rMark.getCumAvg());
                run.setEndTime(new Date());
            }

            EntityManager em = EM.getEntityManager();
            em.getTransaction().begin();
            em.persist(run);
            em.getTransaction().commit();
            Gui.runPanel.addRun(run);
        }

        App.nextMarkNumber += App.numOfMarks;
        return true;
    }

    @Override
    public void stopBenchmark() {

    }

    @Override
    public List<RunParameters> getAllRuns() {
        List<DiskRun> runs = em
                .createNamedQuery("DiskRun.findAll", DiskRun.class)
                .getResultList();
        return runs.stream()
                .map(DiskRun::toRunParameters)
                .collect(Collectors.toList());
    }
}