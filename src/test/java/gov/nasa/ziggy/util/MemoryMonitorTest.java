package gov.nasa.ziggy.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link MemoryMonitor}.
 */
public class MemoryMonitorTest {

    private static final int MiB = 1024 * 1024;
    private static final long SECOND = 1000L;
    private static final int CHUNK_SIZE = 100 * MiB;
    private MemoryMonitor monitor;

    @Before
    public void setup() {
        monitor = new MemoryMonitor(CHUNK_SIZE, 1 * SECOND);
        System.out.println("Monitored pool name: " + monitor.getMonitoredPoolName());
        monitor.startMonitoring();
    }

    @After
    public void teardown() {
        monitor.stopMonitoring();
    }

    @Test
    public void testNoAllocation() {
        assertFalse(monitor.isThresholdExceeded());
    }

    @Test
    public void testOutOfMemory() {
        byte[][] chunks = MemoryTestUtils.allocateAllChunksPossible(CHUNK_SIZE);

        System.out
            .println(String.format("Allocated %d MiB", (long) chunks.length * CHUNK_SIZE / MiB));
        assertTrue(monitor.isThresholdExceeded());

        // Free up the allocated chunks and test that we are no longer under
        // the threshold.
        chunks = null;
        assertFalse(monitor.isThresholdExceeded());
    }

}
