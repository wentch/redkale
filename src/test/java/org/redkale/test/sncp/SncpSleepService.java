/*
 *
 */
package org.redkale.test.sncp;

import java.util.concurrent.CompletableFuture;
import org.redkale.service.AbstractService;
import org.redkale.util.Utility;

/**
 *
 * @author zhangjx
 */
public class SncpSleepService extends AbstractService {

    public CompletableFuture<String> sleep200() {
        return (CompletableFuture) CompletableFuture.supplyAsync(() -> {
            Utility.sleep(200);
            System.out.println("执行完sleep200");
            return "ok200";
        });
    }

    public CompletableFuture<String> sleep300() {
        return (CompletableFuture) CompletableFuture.supplyAsync(() -> {
            Utility.sleep(300);
            System.out.println("执行完sleep300");
            return "ok300";
        });
    }

    public CompletableFuture<String> sleep500() {
        return (CompletableFuture) CompletableFuture.supplyAsync(() -> {
            Utility.sleep(500);
            System.out.println("执行完sleep500");
            return "ok500";
        });
    }
}
