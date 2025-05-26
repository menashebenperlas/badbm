package edu.touro.mco152.bm;

import edu.touro.mco152.bm.persist.DiskRun;

/**
 * A simple DTO that holds the parameters/results of one run.
 */
public class RunParameters {
    private final int numFiles;
    private final int numBlocks;
    private final int blockSizeKb;
    private final DiskRun.BlockSequence blockSequence;
    private final boolean readTest;
    private final boolean writeTest;

    public RunParameters(int numFiles,
                         int numBlocks,
                         int blockSizeKb,
                         DiskRun.BlockSequence blockSequence,
                         boolean readTest,
                         boolean writeTest) {
        this.numFiles     = numFiles;
        this.numBlocks    = numBlocks;
        this.blockSizeKb  = blockSizeKb;
        this.blockSequence= blockSequence;
        this.readTest     = readTest;
        this.writeTest    = writeTest;
    }

    // ======== getters ========
    public int getNumFiles() {
        return numFiles;
    }

    public int getNumBlocks() {
        return numBlocks;
    }

    public int getBlockSizeKb() {
        return blockSizeKb;
    }

    public DiskRun.BlockSequence getBlockSequence() {
        return blockSequence;
    }

    public boolean isReadTest() {
        return readTest;
    }

    public boolean isWriteTest() {
        return writeTest;
    }
}