/*
 *
 */
package org.redkale.mq;

import org.redkale.annotation.Component;
import org.redkale.asm.AsmDepends;
import org.redkale.service.Local;
import org.redkale.util.AnyValue;

/**
 * MQ消费器， 实现类必须标记{@link org.redkale.mq.ResourceConsumer}
 *
 * <p>
 * 详情见: https://redkale.org
 *
 * @author zhangjx
 * @param <T> 泛型
 *
 * @since 2.8.0
 */
@Local
@Component
@AsmDepends
public interface MessageConsumer<T> {

    default void init(AnyValue config) {
    }

    public void onMessage(MessageConext context, T message);

    default void destroy(AnyValue config) {
    }

}
