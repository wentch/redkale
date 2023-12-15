/*
 *
 */
package org.redkale.cache.support;

import org.redkale.boot.Application;
import org.redkale.boot.ModuleEngine;
import org.redkale.util.AnyValue;

/**
 *
 * @author zhangjx
 */
public class CacheModuleEngine extends ModuleEngine {

    //全局缓存管理器
    private CacheManagerService cacheManager;

    public CacheModuleEngine(Application application) {
        super(application);
    }

    /**
     * 结束Application.init方法前被调用
     */
    public void onAppPostInit() {
        //设置缓存管理器
        this.cacheManager = CacheManagerService.create(null).enabled(false);
        final AnyValue cacheConf = environment.getAnyValue("redkale.cache", false);
        this.resourceFactory.inject(this.cacheManager);
        if (!application.isCompileMode() && cacheConf != null) {
            this.cacheManager.init(cacheConf);
        }
        this.resourceFactory.register("", this.cacheManager);
    }

    /**
     * 进入Application.shutdown方法被调用
     */
    public void onAppPreShutdown() {
        if (!application.isCompileMode()) {
            this.cacheManager.destroy(this.cacheManager.getConfig());
        }
    }
}
