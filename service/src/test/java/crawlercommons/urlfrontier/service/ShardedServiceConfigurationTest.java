// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import crawlercommons.urlfrontier.service.rocksdb.ShardedRocksDBService;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

/**
 * The sharded service must build its own executors from the configuration: they are the ones
 * serving getURLs and putURLs, the ones of the wrapped instance are never used.
 *
 * <p>Lives in the same package as {@link AbstractFrontierService} so that the executors can be read
 * directly.
 */
class ShardedServiceConfigurationTest {

    private static final int PORT = 7404;
    private static final String PATH = "./target/rocksdb-sharded-conf";

    @Test
    void threadPoolSettingsAreHonoured() throws Exception {
        FileUtils.deleteQuietly(new File(PATH));
        final Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.path", PATH);
        conf.put("nodes", "localhost:" + PORT);
        conf.put("read.thread.num", "7");
        conf.put("write.thread.num", "9");

        ShardedRocksDBService service = new ShardedRocksDBService(conf, "localhost", PORT);
        try {
            assertEquals(
                    7,
                    ((ThreadPoolExecutor) service.readExecutorService).getCorePoolSize(),
                    "read.thread.num must be applied to the sharded service");
            assertEquals(
                    9,
                    ((ThreadPoolExecutor) service.writeExecutorService).getCorePoolSize(),
                    "write.thread.num must be applied to the sharded service");
        } finally {
            service.close();
            FileUtils.deleteQuietly(new File(PATH));
        }
    }
}
