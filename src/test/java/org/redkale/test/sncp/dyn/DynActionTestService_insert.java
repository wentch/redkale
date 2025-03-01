/*
 * Copyright (c) 2016-2116 Redkale
 * All rights reserved.
 */
package org.redkale.test.sncp.dyn;

import java.lang.reflect.Method;
import org.redkale.convert.Convert;
import org.redkale.convert.ConvertColumn;
import org.redkale.convert.Reader;
import org.redkale.convert.Writer;
import org.redkale.net.sncp.SncpActionServlet;
import org.redkale.net.sncp.SncpRequest;
import org.redkale.net.sncp.SncpResponse;
import org.redkale.service.Service;
import org.redkale.test.util.TestBean;
import org.redkale.util.Uint128;

/**
 *
 * @author zhangjx
 */
public class DynActionTestService_insert extends SncpActionServlet {

    public DynActionTestService_insert(
            String resourceName,
            Class resourceType,
            Service service,
            Uint128 serviceid,
            Uint128 actionid,
            final Method method) {
        super(resourceName, resourceType, service, serviceid, actionid, method);
    }

    @Override
    public void action(SncpRequest request, SncpResponse response) throws Throwable {
        Convert<Reader, Writer> convert = request.getConvert();
        Reader in = request.getReader();
        DynSncpActionParamBean_TestService_insert bean = convert.convertFrom(paramComposeBeanType, in);
        bean.arg1 = response.getParamAsyncHandler();
        TestService serviceObj = (TestService) service();
        serviceObj.insert(bean.arg1, bean.arg2, bean.arg3, bean.arg4);
        response.finishVoid();
    }

    public static class DynSncpActionParamBean_TestService_insert {

        public DynSncpActionParamBean_TestService_insert() {}

        public DynSncpActionParamBean_TestService_insert(Object[] params) {
            this.arg1 = (BooleanHandler) params[0];
            this.arg2 = (TestBean) params[1];
            this.arg3 = (String) params[2];
            this.arg4 = (int) params[3];
        }

        @ConvertColumn(index = 1)
        public BooleanHandler arg1;

        @ConvertColumn(index = 2)
        public TestBean arg2;

        @ConvertColumn(index = 3)
        public String arg3;

        @ConvertColumn(index = 4)
        public int arg4;
    }
}
