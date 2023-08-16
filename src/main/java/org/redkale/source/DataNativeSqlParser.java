/*
 *
 */
package org.redkale.source;

import java.util.*;
import java.util.function.Function;
import org.redkale.convert.ConvertDisabled;
import org.redkale.convert.json.JsonConvert;

/**
 *
 * 原生的sql解析器
 *
 * <p>
 * 详情见: https://redkale.org
 *
 * @author zhangjx
 * @since 2.8.0
 */
public interface DataNativeSqlParser {

    NativeSqlStatement parse(Function<Integer, String> signFunc, String dbtype, String nativeSql, Map<String, Object> params);

    public static class NativeSqlStatement {

        //根据参数值集合重新生成的带?参数可执行的sql
        protected String nativeSql;

        //根据参数值集合重新生成的带?参数可执行的计算总数sql,用于返回Sheet对象
        protected String nativeCountSql;

        //是否包含InExpression参数名
        protected boolean existInNamed;

        //需要预编译的参数名, 数量与sql中的?数量一致
        protected List<String> paramNames;

        //参数值集合, paramNames中的key必然会存在
        protected Map<String, Object> paramValues;

        /**
         * 是否带有参数
         *
         * @return 是否带有参数
         */
        @ConvertDisabled
        public boolean isEmptyNamed() {
            return paramNames == null || paramNames.isEmpty();
        }

        public String getNativeSql() {
            return nativeSql;
        }

        public void setNativeSql(String nativeSql) {
            this.nativeSql = nativeSql;
        }

        public String getNativeCountSql() {
            return nativeCountSql;
        }

        public void setNativeCountSql(String nativeCountSql) {
            this.nativeCountSql = nativeCountSql;
        }

        public boolean isExistInNamed() {
            return existInNamed;
        }

        public void setExistInNamed(boolean existInNamed) {
            this.existInNamed = existInNamed;
        }

        public List<String> getParamNames() {
            return paramNames;
        }

        public void setParamNames(List<String> paramNames) {
            this.paramNames = paramNames;
        }

        public Map<String, Object> getParamValues() {
            return paramValues;
        }

        public void setParamValues(Map<String, Object> paramValues) {
            this.paramValues = paramValues;
        }

        @Override
        public String toString() {
            return JsonConvert.root().convertTo(this);
        }
    }
}
