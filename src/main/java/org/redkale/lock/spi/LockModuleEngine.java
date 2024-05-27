/*
 *
 */
package org.redkale.lock.spi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import org.redkale.asm.AsmMethodBoost;
import org.redkale.boot.Application;
import org.redkale.boot.ModuleEngine;
import org.redkale.lock.LockManager;
import org.redkale.service.Service;
import org.redkale.util.AnyValue;
import org.redkale.util.InstanceProvider;
import org.redkale.util.RedkaleClassLoader;

/** @author zhangjx */
public class LockModuleEngine extends ModuleEngine {

	// 全局锁管理器
	private LockManager lockManager;

	private AnyValue config;

	public LockModuleEngine(Application application) {
		super(application);
	}

	/**
	 * 判断模块的配置项合并策略， 返回null表示模块不识别此配置项
	 *
	 * @param path 配置项路径
	 * @param key 配置项名称
	 * @param val1 配置项原值
	 * @param val2 配置项新值
	 * @return MergeEnum
	 */
	@Override
	public AnyValue.MergeEnum mergeAppConfigStrategy(String path, String key, AnyValue val1, AnyValue val2) {
		if ("".equals(path) && "lock".equals(key)) {
			return AnyValue.MergeEnum.REPLACE;
		}
		return null;
	}

	/**
	 * 动态扩展类的方法
	 *
	 * @param remote 是否远程模式
	 * @param serviceClass 类
	 * @return 方法动态扩展器
	 */
	public AsmMethodBoost createAsmMethodBoost(boolean remote, Class serviceClass) {
		return new LockAsmMethodBoost(remote, serviceClass);
	}

	/** 结束Application.init方法前被调用 */
	@Override
	public void onAppPostInit() {
		// 设置锁管理器
		this.config = application.getAppConfig().getAnyValue("lock");
		this.lockManager = createManager(this.config);
		if (!application.isCompileMode()) {
			this.resourceFactory.inject(this.lockManager);
			if (this.lockManager instanceof Service) {
				((Service) this.lockManager).init(this.config);
			}
		}
		this.resourceFactory.register("", LockManager.class, this.lockManager);
	}

	/** 进入Application.shutdown方法被调用 */
	@Override
	public void onAppPreShutdown() {
		if (!application.isCompileMode() && this.lockManager instanceof Service) {
			((Service) this.lockManager).destroy(this.config);
		}
	}

	private LockManager createManager(AnyValue conf) {
		Iterator<LockManagerProvider> it = ServiceLoader.load(LockManagerProvider.class, application.getClassLoader())
				.iterator();
		RedkaleClassLoader.putServiceLoader(LockManagerProvider.class);
		List<LockManagerProvider> providers = new ArrayList<>();
		while (it.hasNext()) {
			LockManagerProvider provider = it.next();
			if (provider != null && provider.acceptsConf(conf)) {
				RedkaleClassLoader.putReflectionPublicConstructors(
						provider.getClass(), provider.getClass().getName());
				providers.add(provider);
			}
		}
		for (LockManagerProvider provider : InstanceProvider.sort(providers)) {
			return provider.createInstance();
		}
		return LockManagerService.create(null).enabled(false);
	}
}
