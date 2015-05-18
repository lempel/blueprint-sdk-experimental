package blueprint.sdk.experimental.filesystem.transactional;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AcidFileSystemTest extends AcidFileSystem {

    private static final Logger L = LoggerFactory.getLogger(AcidFileSystemTest.class);

    @SuppressWarnings("FieldCanBeLocal")
    private static final boolean verbose = true;

    private static final int NUM_THREADS = 10;

    private static final int WORK_DURATION = 60 * 1000;
    private static final int IDLE_DURATION = 10 * 1000;

    private AcidFileSystemTest() {
        super();
    }

    public static void main(String[] args) {
        new AcidFileSystemTest().test();
    }

    void test() {
        TestThread[] threads = new TestThread[NUM_THREADS];

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new TestThread(i, this, WORK_DURATION, IDLE_DURATION);
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
    }

    class TestThread extends Thread {
        private static final String PATH1 = "e:\\10mb.txt";
        private static final String PATH2 = "e:\\10mb.org.txt";
        final AcidFileSystemTest fs;
        final long work;
        final long idle;
        private final byte[] contents_10mb;

        @SuppressWarnings("SameParameterValue")
        public TestThread(int id, AcidFileSystemTest fs, long work, long idle) {
            this.fs = fs;
            this.work = work;
            this.idle = idle;

            setName(Integer.toString(id));
            byte[] proto = Integer.toString(id).getBytes();
            contents_10mb = new byte[10 * 1024 * 1024];
            for (int i = 0; i < contents_10mb.length; i += proto.length) {
                if (i + proto.length < contents_10mb.length) {
                    System.arraycopy(proto, 0, contents_10mb, i, proto.length);
                } else {
                    System.arraycopy(proto, 0, contents_10mb, i, contents_10mb.length - i);
                }

                contents_10mb[i] = proto[0];
            }
        }

        public void run() {
            long start = System.currentTimeMillis();

            while (true) {
                long now = System.currentTimeMillis();

                if (now >= start + work) {
                    L.debug(hashCode() + " now waiting");

                    try {
                        sleep(idle);
                    } catch (InterruptedException ignored) {
                    }

                    L.debug(hashCode() + " now working");

                    start = System.currentTimeMillis();
                }

                Transaction tr = fs.newTransaction();

                try {
                    if (verbose)
                        L.debug(hashCode() + " write 10mb");
                    tr.writeToFile(PATH1, contents_10mb, false);
                    if (verbose)
                        L.debug(hashCode() + " write 10mb - ok");

                    if (verbose)
                        L.debug(hashCode() + " delete 10mb.org");
                    tr.deleteFile(PATH2);
                    if (verbose)
                        L.debug(hashCode() + " delete 10mb.org - ok");

                    if (verbose)
                        L.debug(hashCode() + " rename 10mb > 10mb.org");
                    tr.renameFile(PATH1, PATH2);
                    if (verbose)
                        L.debug(hashCode() + " rename 10mb > 10mb.org - ok");

                    if (verbose)
                        L.debug(hashCode() + " read 10mb.org");
                    byte[] data = tr.readFile(PATH2);
                    if (verbose)
                        L.debug(hashCode() + " read 10mb.org - ok");

                    if (data == null) {
                        throw new NullPointerException("null file");
                    }
                    if (data.length != contents_10mb.length) {
                        throw new IOException("mismatched read size. data.length=" + data.length);
                    }

                    tr.commit();
                } catch (IOException e) {
                    L.debug(hashCode() + " - " + e, e);

                    tr.rollback();

                    System.exit(100);
                }
            }
        }
    }
}
