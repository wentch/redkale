/*
 */
package org.redkale.source;

import org.redkale.annotation.AutoLoad;
import org.redkale.annotation.ResourceListener;
import org.redkale.annotation.ResourceType;
import org.redkale.service.*;
import org.redkale.util.*;

/**
 * CacheSource的S抽象实现类 <br>
 *
 *
 * 详情见: https://redkale.org
 *
 * @author zhangjx
 * @since 2.7.0
 */
@Local
@AutoLoad(false)
@SuppressWarnings("unchecked")
@ResourceType(CacheSource.class)
public abstract class AbstractCacheSource extends AbstractService implements CacheSource, AutoCloseable, Resourcable {

    //@since 2.7.0
    public static final String CACHE_SOURCE_URL = "url";

    //@since 2.7.0
    public static final String CACHE_SOURCE_DB = "db";

    //@since 2.7.0
    public static final String CACHE_SOURCE_USER = "user";

    //@since 2.7.0
    public static final String CACHE_SOURCE_PASSWORD = "password";

    //@since 2.7.0
    public static final String CACHE_SOURCE_ENCODING = "encoding";

    //@since 2.7.0
    public static final String CACHE_SOURCE_NODE = "node";

    //@since 2.7.0
    public static final String CACHE_SOURCE_MAXCONNS = "maxconns";

    //@since 2.7.0
    public static final String CACHE_SOURCE_PIPELINES = "pipelines";

    @ResourceListener
    public abstract void onResourceChange(ResourceEvent[] events);
}
