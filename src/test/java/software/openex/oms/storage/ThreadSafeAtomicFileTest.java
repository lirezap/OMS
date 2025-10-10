package software.openex.oms.storage;

import org.junit.jupiter.api.Test;
import software.openex.oms.binary.file.FileHeader;
import software.openex.oms.binary.file.FileHeaderBinaryRepresentation;

import java.lang.foreign.Arena;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alireza Pourtaghi
 */
public class ThreadSafeAtomicFileTest {
    private static final SecureRandom random = new SecureRandom();

    @Test
    public void testAppend() throws Exception {
        var pool = Executors.newWorkStealingPool(8);
        var path = Path.of("/tmp/" + System.currentTimeMillis() + ".test");
        var bufferSize = random.nextInt(512, 1024 + 1);
        var sleepTime = random.nextInt(500, 1000 + 1);
        var succeeded = new AtomicInteger(0);

        try (var arena = Arena.ofShared();
             var file = new ThreadSafeAtomicFile(path, 5000)) {

            var segment = arena.allocate(bufferSize);
            for (int i = 1; i <= 1000000; i++) {
                pool.submit(() -> {
                    file.append(segment.asByteBuffer());
                    succeeded.incrementAndGet();
                });
            }

            Thread.sleep(sleepTime);
            pool.shutdownNow();
        }

        try (var header = new FileHeaderBinaryRepresentation(new FileHeader(0));
             var file = new ThreadSafeAtomicFile(path, 5000)) {

            assertEquals(0, (file.source().toFile().length() - header.representationSize()) % bufferSize);
            assertEquals(succeeded.get(), (file.source().toFile().length() - header.representationSize()) / bufferSize);
        }
    }
}
