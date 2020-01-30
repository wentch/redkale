/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.source;

import java.io.Serializable;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.*;
import javax.persistence.*;
import org.redkale.convert.json.*;
import org.redkale.util.*;

/**
 * Entity操作类
 *
 * <p>
 * 详情见: https://redkale.org
 *
 * @author zhangjx
 * @param <T> Entity类的泛型
 */
@SuppressWarnings("unchecked")
public final class EntityInfo<T> {

    private static final JsonConvert DEFAULT_JSON_CONVERT = JsonFactory.create().skipAllIgnore(true).getConvert();

    //全局静态资源
    private static final ConcurrentHashMap<Class, EntityInfo> entityInfos = new ConcurrentHashMap<>();

    //日志
    private static final Logger logger = Logger.getLogger(EntityInfo.class.getSimpleName());

    //Entity类名
    private final Class<T> type;

    //类对应的数据表名, 如果是VirtualEntity 类， 则该字段为null
    final String table;

    //JsonConvert
    final JsonConvert jsonConvert;

    //Entity构建器
    private final Creator<T> creator;

    //Entity构建器参数
    private final String[] constructorParameters;

    //Entity构建器参数Attribute
    private final Attribute<T, Serializable>[] constructorAttributes;

    //Entity构建器参数Attribute
    private final Attribute<T, Serializable>[] unconstructorAttributes;

    //主键
    final Attribute<T, Serializable> primary;

    //Entity缓存对象
    private final EntityCache<T> cache;

    //key是field的name， 不是sql字段。
    //存放所有与数据库对应的字段， 包括主键
    private final HashMap<String, Attribute<T, Serializable>> attributeMap = new HashMap<>();

    //存放所有与数据库对应的字段， 包括主键
    final Attribute<T, Serializable>[] attributes;

    //key是field的name， value是Column的别名，即数据库表的字段名
    //只有field.name 与 Column.name不同才存放在aliasmap里.
    private final Map<String, String> aliasmap;

    //key是field的name， value是CryptHandler
    //字段都不存在CryptHandler时值因为为null，减少判断
    private final Map<String, CryptHandler> cryptmap;

    //所有可更新字段，即排除了主键字段和标记为&#064;Column(updatable=false)的字段
    private final Map<String, Attribute<T, Serializable>> updateAttributeMap = new HashMap<>();

    //用于反向LIKE使用
    final String containSQL;

    //用于反向LIKE使用
    final String notcontainSQL;

    //用于判断表不存在的使用, 多个SQLState用;隔开
    private final String tablenotexistSqlstates;

    //用于复制表结构使用
    private final String tablecopySQL;

    //用于存在database.table_20160202类似这种分布式表
    private final Set<String> tables = new CopyOnWriteArraySet<>();

    //不能为null的字段名
    private final Set<String> notNullColumns = new CopyOnWriteArraySet<>();

    //分表 策略
    private final DistributeTableStrategy<T> tableStrategy;

    //根据主键查找单个对象的SQL， 含 ？
    private final String queryPrepareSQL;

    //根据主键查找单个对象的SQL， 含 $
    private final String queryDollarPrepareSQL;

    //根据主键查找单个对象的SQL， 含 :name
    private final String queryNamesPrepareSQL;

    //数据库中所有字段
    private final Attribute<T, Serializable>[] queryAttributes;

    //新增SQL， 含 ？，即排除了自增长主键和标记为&#064;Column(insertable=false)的字段
    private final String insertPrepareSQL;

    //新增SQL， 含 $，即排除了自增长主键和标记为&#064;Column(insertable=false)的字段    
    private final String insertDollarPrepareSQL;

    //新增SQL， 含 :name，即排除了自增长主键和标记为&#064;Column(insertable=false)的字段
    private final String insertNamesPrepareSQL;

    //数据库中所有可新增字段
    final Attribute<T, Serializable>[] insertAttributes;

    //根据主键更新所有可更新字段的SQL，含 ？
    private final String updatePrepareSQL;

    //根据主键更新所有可更新字段的SQL，含 $
    private final String updateDollarPrepareSQL;

    //根据主键更新所有可更新字段的SQL，含 :name
    private final String updateNamesPrepareSQL;

    //数据库中所有可更新字段
    final Attribute<T, Serializable>[] updateAttributes;

    //根据主键删除记录的SQL，含 ？
    private final String deletePrepareSQL;

    //根据主键删除记录的SQL，含 $
    private final String deleteDollarPrepareSQL;

    //根据主键删除记录的SQL，含 :name
    private final String deleteNamesPrepareSQL;

    //日志级别，从LogLevel获取
    private final int logLevel;

    //日志控制
    private final Map<Integer, String[]> excludeLogLevels;

    //Flipper.sort转换成以ORDER BY开头SQL的缓存
    private final Map<String, String> sortOrderbySqls = new ConcurrentHashMap<>();

    //所属的DataSource
    final DataSource source;

    //全量数据的加载器
    final BiFunction<DataSource, Class, List> fullloader;
    //------------------------------------------------------------

    /**
     * 加载EntityInfo
     *
     * @param clazz          Entity类
     * @param cacheForbidden 是否禁用EntityCache
     * @param conf           配置信息, persistence.xml中的property节点值
     * @param source         DataSource,可为null
     * @param fullloader     全量加载器,可为null
     */
    static <T> EntityInfo<T> load(Class<T> clazz, final boolean cacheForbidden, final Properties conf,
        DataSource source, BiFunction<DataSource, Class, List> fullloader) {
        EntityInfo rs = entityInfos.get(clazz);
        if (rs != null && (rs.cache == null || rs.cache.isFullLoaded())) return rs;
        synchronized (entityInfos) {
            rs = entityInfos.get(clazz);
            if (rs == null) {
                rs = new EntityInfo(clazz, cacheForbidden, conf, source, fullloader);
                entityInfos.put(clazz, rs);
                if (rs.cache != null) {
                    if (fullloader == null) throw new IllegalArgumentException(clazz.getName() + " auto loader  is illegal");
                    rs.cache.fullLoad();
                }
            }
            return rs;
        }
    }

    /**
     * 获取Entity类对应的EntityInfo对象
     *
     * @param <T>   泛型
     * @param clazz Entity类
     *
     * @return EntityInfo
     */
    static <T> EntityInfo<T> get(Class<T> clazz) {
        return entityInfos.get(clazz);
    }

    /**
     * 构造函数
     *
     * @param type           Entity类
     * @param cacheForbidden 是否禁用EntityCache
     * @param conf           配置信息, persistence.xml中的property节点值
     * @param source         DataSource,可为null
     * @param fullloader     全量加载器,可为null
     */
    private EntityInfo(Class<T> type, final boolean cacheForbidden,
        Properties conf, DataSource source, BiFunction<DataSource, Class, List> fullloader) {
        this.type = type;
        this.source = source;
        //---------------------------------------------

        LogLevel ll = type.getAnnotation(LogLevel.class);
        this.logLevel = ll == null ? Integer.MIN_VALUE : Level.parse(ll.value()).intValue();
        Map<Integer, HashSet<String>> logmap = new HashMap<>();
        for (LogExcludeLevel lel : type.getAnnotationsByType(LogExcludeLevel.class)) {
            for (String onelevel : lel.levels()) {
                int level = Level.parse(onelevel).intValue();
                HashSet<String> set = logmap.get(level);
                if (set == null) {
                    set = new HashSet<>();
                    logmap.put(level, set);
                }
                for (String key : lel.keys()) {
                    set.add(key);
                }
            }
        }
        if (logmap.isEmpty()) {
            this.excludeLogLevels = null;
        } else {
            this.excludeLogLevels = new HashMap<>();
            logmap.forEach((l, set) -> excludeLogLevels.put(l, set.toArray(new String[set.size()])));
        }
        //---------------------------------------------
        Table t = type.getAnnotation(Table.class);
        if (type.getAnnotation(VirtualEntity.class) != null || (source == null || "memory".equalsIgnoreCase(source.getType()))) {
            this.table = null;
            BiFunction<DataSource, Class, List> loader = null;
            try {
                VirtualEntity ve = type.getAnnotation(VirtualEntity.class);
                if (ve != null) loader = ve.loader().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                logger.log(Level.SEVERE, type + " init @VirtualEntity.loader error", e);
            }
            this.fullloader = loader;
        } else {
            this.fullloader = fullloader;
            if (t != null && !t.name().isEmpty() && t.name().indexOf('.') >= 0) throw new RuntimeException(type + " have illegal table.name on @Table");
            this.table = (t == null) ? type.getSimpleName().toLowerCase() : (t.catalog().isEmpty()) ? (t.name().isEmpty() ? type.getSimpleName().toLowerCase() : t.name()) : (t.catalog() + '.' + (t.name().isEmpty() ? type.getSimpleName().toLowerCase() : t.name()));
        }
        DistributeTable dt = type.getAnnotation(DistributeTable.class);
        DistributeTableStrategy dts = null;
        try {
            dts = (dt == null) ? null : dt.strategy().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.log(Level.SEVERE, type + " init DistributeTableStrategy error", e);
        }
        this.tableStrategy = dts;

        this.creator = Creator.create(type);
        ConstructorParameters cp = null;
        try {
            cp = this.creator.getClass().getMethod("create", Object[].class).getAnnotation(ConstructorParameters.class);
        } catch (Exception e) {
            logger.log(Level.SEVERE, type + " cannot find ConstructorParameters Creator", e);
        }
        this.constructorParameters = (cp == null || cp.value().length < 1) ? null : cp.value();
        Attribute idAttr0 = null;
        Map<String, CryptHandler> cryptmap0 = null;
        Map<String, String> aliasmap0 = null;
        Class cltmp = type;
        Set<String> fields = new HashSet<>();
        List<Attribute<T, Serializable>> queryattrs = new ArrayList<>();
        List<String> insertcols = new ArrayList<>();
        List<Attribute<T, Serializable>> insertattrs = new ArrayList<>();
        List<String> updatecols = new ArrayList<>();
        List<Attribute<T, Serializable>> updateattrs = new ArrayList<>();
        Map<Class, Creator<CryptHandler>> cryptCreatorMap = new HashMap<>();
        do {
            for (Field field : cltmp.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (Modifier.isFinal(field.getModifiers())) continue;
                if (field.getAnnotation(Transient.class) != null) continue;
                if (fields.contains(field.getName())) continue;
                final String fieldname = field.getName();
                final Column col = field.getAnnotation(Column.class);
                final String sqlfield = col == null || col.name().isEmpty() ? fieldname : col.name();
                if (!fieldname.equals(sqlfield)) {
                    if (aliasmap0 == null) aliasmap0 = new HashMap<>();
                    aliasmap0.put(fieldname, sqlfield);
                }
                final CryptColumn cpt = field.getAnnotation(CryptColumn.class);
                CryptHandler cryptHandler = null;
                if (cpt != null) {
                    if (cryptmap0 == null) cryptmap0 = new HashMap<>();
                    cryptHandler = cryptCreatorMap.computeIfAbsent(cpt.handler(), c -> (Creator<CryptHandler>) Creator.create(cpt.handler())).create();
                    cryptmap0.put(fieldname, cryptHandler);
                }
                Attribute attr;
                try {
                    attr = Attribute.create(type, cltmp, field, cryptHandler);
                } catch (RuntimeException e) {
                    continue;
                }
                if (field.getAnnotation(javax.persistence.Id.class) != null && idAttr0 == null) {
                    idAttr0 = attr;
                    insertcols.add(sqlfield);
                    insertattrs.add(attr);
                } else {
                    if (col == null || col.insertable()) {
                        insertcols.add(sqlfield);
                        insertattrs.add(attr);
                    }
                    if (col == null || col.updatable()) {
                        updatecols.add(sqlfield);
                        updateattrs.add(attr);
                        updateAttributeMap.put(fieldname, attr);
                    }
                    if (col != null && !col.nullable()) {
                        notNullColumns.add(fieldname);
                    }
                }
                queryattrs.add(attr);
                fields.add(fieldname);
                attributeMap.put(fieldname, attr);
            }
        } while ((cltmp = cltmp.getSuperclass()) != Object.class);
        if (idAttr0 == null) throw new RuntimeException(type.getName() + " have no primary column by @javax.persistence.Id");
        cltmp = type;
        JsonConvert convert = DEFAULT_JSON_CONVERT;
        do {
            for (Method method : cltmp.getDeclaredMethods()) {
                if (method.getAnnotation(SourceConvert.class) == null) continue;
                if (!Modifier.isStatic(method.getModifiers())) throw new RuntimeException("@SourceConvert method(" + method + ") must be static");
                if (method.getReturnType() != JsonConvert.class) throw new RuntimeException("@SourceConvert method(" + method + ") must be return JsonConvert.class");
                if (method.getParameterCount() > 0) throw new RuntimeException("@SourceConvert method(" + method + ") must be 0 parameter");
                try {
                    method.setAccessible(true);
                    convert = (JsonConvert) method.invoke(null);
                } catch (Exception e) {
                    throw new RuntimeException(method + " invoke error", e);
                }
                if (convert != null) break;
            }
        } while ((cltmp = cltmp.getSuperclass()) != Object.class);
        this.jsonConvert = convert == null ? DEFAULT_JSON_CONVERT : convert;

        this.primary = idAttr0;
        this.aliasmap = aliasmap0;
        this.cryptmap = cryptmap0;
        this.attributes = attributeMap.values().toArray(new Attribute[attributeMap.size()]);
        this.queryAttributes = queryattrs.toArray(new Attribute[queryattrs.size()]);
        this.insertAttributes = insertattrs.toArray(new Attribute[insertattrs.size()]);
        this.updateAttributes = updateattrs.toArray(new Attribute[updateattrs.size()]);
        if (this.constructorParameters == null) {
            this.constructorAttributes = null;
            this.unconstructorAttributes = null;
        } else {
            this.constructorAttributes = new Attribute[this.constructorParameters.length];
            List<Attribute<T, Serializable>> unconstructorAttrs = new ArrayList<>();
            for (Attribute<T, Serializable> attr : queryAttributes) {
                int pos = -1;
                for (int i = 0; i < this.constructorParameters.length; i++) {
                    if (attr.field().equals(this.constructorParameters[i])) {
                        pos = i;
                        break;
                    }
                }
                if (pos >= 0) {
                    this.constructorAttributes[pos] = attr;
                } else {
                    unconstructorAttrs.add(attr);
                }
            }
            this.unconstructorAttributes = unconstructorAttrs.toArray(new Attribute[unconstructorAttrs.size()]);
        }
        if (table != null) {
            StringBuilder insertsb = new StringBuilder();
            StringBuilder insertsbjdbc = new StringBuilder();
            StringBuilder insertsbdollar = new StringBuilder();
            StringBuilder insertsbnames = new StringBuilder();
            int index = 0;
            for (String col : insertcols) {
                if (index > 0) insertsb.append(',');
                insertsb.append(col);
                if (index > 0) {
                    insertsbjdbc.append(',');
                    insertsbdollar.append(',');
                    insertsbnames.append(',');
                }
                insertsbjdbc.append('?');
                insertsbdollar.append("$").append(++index);
                insertsbnames.append(":").append(col);
            }
            this.insertPrepareSQL = "INSERT INTO " + (this.tableStrategy == null ? table : "${newtable}") + "(" + insertsb + ") VALUES(" + insertsbjdbc + ")";
            this.insertDollarPrepareSQL = "INSERT INTO " + (this.tableStrategy == null ? table : "${newtable}") + "(" + insertsb + ") VALUES(" + insertsbdollar + ")";
            this.insertNamesPrepareSQL = "INSERT INTO " + (this.tableStrategy == null ? table : "${newtable}") + "(" + insertsb + ") VALUES(" + insertsbnames + ")";
            StringBuilder updatesb = new StringBuilder();
            StringBuilder updatesbdollar = new StringBuilder();
            StringBuilder updatesbnames = new StringBuilder();
            index = 0;
            for (String col : updatecols) {
                if (index > 0) {
                    updatesb.append(", ");
                    updatesbdollar.append(", ");
                    updatesbnames.append(", ");
                }
                updatesb.append(col).append(" = ?");
                updatesbdollar.append(col).append(" = ").append("$").append(++index);
                updatesbnames.append(col).append(" = :").append(col);
            }
            this.updatePrepareSQL = "UPDATE " + (this.tableStrategy == null ? table : "${newtable}") + " SET " + updatesb + " WHERE " + getPrimarySQLColumn(null) + " = ?";
            this.updateDollarPrepareSQL = "UPDATE " + (this.tableStrategy == null ? table : "${newtable}") + " SET " + updatesbdollar + " WHERE " + getPrimarySQLColumn(null) + " = $" + (++index);
            this.updateNamesPrepareSQL = "UPDATE " + (this.tableStrategy == null ? table : "${newtable}") + " SET " + updatesbnames + " WHERE " + getPrimarySQLColumn(null) + " = :" + getPrimarySQLColumn(null);
            this.deletePrepareSQL = "DELETE FROM " + (this.tableStrategy == null ? table : "${newtable}") + " WHERE " + getPrimarySQLColumn(null) + " = ?";
            this.deleteDollarPrepareSQL = "DELETE FROM " + (this.tableStrategy == null ? table : "${newtable}") + " WHERE " + getPrimarySQLColumn(null) + " = $1";
            this.deleteNamesPrepareSQL = "DELETE FROM " + (this.tableStrategy == null ? table : "${newtable}") + " WHERE " + getPrimarySQLColumn(null) + " = :" + getPrimarySQLColumn(null);
            this.queryPrepareSQL = "SELECT * FROM " + table + " WHERE " + getPrimarySQLColumn(null) + " = ?";
            this.queryDollarPrepareSQL = "SELECT * FROM " + table + " WHERE " + getPrimarySQLColumn(null) + " = $1";
            this.queryNamesPrepareSQL = "SELECT * FROM " + table + " WHERE " + getPrimarySQLColumn(null) + " = :" + getPrimarySQLColumn(null);
        } else {
            this.insertPrepareSQL = null;
            this.updatePrepareSQL = null;
            this.deletePrepareSQL = null;
            this.queryPrepareSQL = null;

            this.insertDollarPrepareSQL = null;
            this.updateDollarPrepareSQL = null;
            this.deleteDollarPrepareSQL = null;
            this.queryDollarPrepareSQL = null;

            this.insertNamesPrepareSQL = null;
            this.updateNamesPrepareSQL = null;
            this.deleteNamesPrepareSQL = null;
            this.queryNamesPrepareSQL = null;
        }
        //----------------cache--------------
        Cacheable c = type.getAnnotation(Cacheable.class);
        if (this.table == null || (!cacheForbidden && c != null && c.value())) {
            this.cache = new EntityCache<>(this, c);
        } else {
            this.cache = null;
        }
        if (conf == null) conf = new Properties();
        this.containSQL = conf.getProperty(DataSources.JDBC_CONTAIN_SQLTEMPLATE, "LOCATE(${keystr}, ${column}) > 0");
        this.notcontainSQL = conf.getProperty(DataSources.JDBC_NOTCONTAIN_SQLTEMPLATE, "LOCATE(${keystr}, ${column}) = 0");

        this.tablenotexistSqlstates = ";" + conf.getProperty(DataSources.JDBC_TABLENOTEXIST_SQLSTATES, "42000;42S02") + ";";
        this.tablecopySQL = conf.getProperty(DataSources.JDBC_TABLECOPY_SQLTEMPLATE, "CREATE TABLE ${newtable} LIKE ${oldtable}");
    }

    /**
     * 获取JsonConvert
     *
     * @return JsonConvert
     */
    public JsonConvert getJsonConvert() {
        return jsonConvert;
    }

    /**
     * 获取Entity缓存器
     *
     * @return EntityCache
     */
    public EntityCache<T> getCache() {
        return cache;
    }

    /**
     * 判断缓存器是否已经全量加载过
     *
     * @return boolean
     */
    public boolean isCacheFullLoaded() {
        return cache != null && cache.isFullLoaded();
    }

    /**
     * 获取Entity构建器
     *
     * @return Creator
     */
    public Creator<T> getCreator() {
        return creator;
    }

    /**
     * 获取Entity类名
     *
     * @return Class
     */
    public Class<T> getType() {
        return type;
    }

    /**
     * 判断Entity是否为虚拟类
     *
     * @return boolean
     */
    public boolean isVirtualEntity() {
        return table == null;
    }

    public DistributeTableStrategy<T> getTableStrategy() {
        return tableStrategy;
    }

    public Object disTableLock() {
        return tables;
    }

    public boolean containsDisTable(String tablekey) {
        return tables.contains(tablekey);
    }

    public void addDisTable(String tablekey) {
        tables.add(tablekey);
    }

    public boolean removeDisTable(String tablekey) {
        return tables.remove(tablekey);
    }

    public String getTableNotExistSqlStates2() {
        return tablenotexistSqlstates;
    }

    public boolean isTableNotExist(String code) {
        return tablenotexistSqlstates.contains(';' + code + ';');
    }

    public boolean isTableNotExist(SQLException e) {
        if (e == null) return false;
        return tablenotexistSqlstates.contains(';' + e.getSQLState() + ';');
    }

    public Attribute<T, Serializable>[] getInsertAttributes() {
        return insertAttributes;
    }

    public Attribute<T, Serializable>[] getUpdateAttributes() {
        return updateAttributes;
    }

    public Attribute<T, Serializable>[] getQueryAttributes() {
        return queryAttributes;
    }

    /**
     * 获取Entity的QUERY SQL
     *
     * @param bean Entity对象
     *
     * @return String
     */
    public String getQueryPrepareSQL(T bean) {
        if (this.tableStrategy == null) return queryPrepareSQL;
        return queryPrepareSQL.replace("${newtable}", getTable(bean));
    }

    /**
     * 获取Entity的QUERY SQL
     *
     * @param bean Entity对象
     *
     * @return String
     */
    public String getQueryDollarPrepareSQL(T bean) {
        if (this.tableStrategy == null) return queryDollarPrepareSQL;
        return queryDollarPrepareSQL.replace("${newtable}", getTable(bean));
    }

    /**
     * 获取Entity的QUERY SQL
     *
     * @param bean Entity对象
     *
     * @return String
     */
    public String getQueryNamesPrepareSQL(T bean) {
        if (this.tableStrategy == null) return queryNamesPrepareSQL;
        return queryNamesPrepareSQL.replace("${newtable}", getTable(bean));
    }

    /**
     * 获取Entity的INSERT SQL
     *
     * @param bean Entity对象
     *
     * @return String
     */
    public String getInsertPrepareSQL(T bean) {
        if (this.tableStrategy == null) return insertPrepareSQL;
        return insertPrepareSQL.replace("${newtable}", getTable(bean));
    }

    /**
     * 获取Entity的INSERT SQL
     *
     * @param bean Entity对象
     *
     * @return String
     */
    public String getInsertDollarPrepareSQL(T bean) {
        if (this.tableStrategy == null) return insertDollarPrepareSQL;
        return insertDollarPrepareSQL.replace("${newtable}", getTable(bean));
    }

    /**
     * 获取Entity的INSERT SQL
     *
     * @param bean Entity对象
     *
     * @return String
     */
    public String getInsertNamesPrepareSQL(T bean) {
        if (this.tableStrategy == null) return insertNamesPrepareSQL;
        return insertNamesPrepareSQL.replace("${newtable}", getTable(bean));
    }

    /**
     * 获取Entity的UPDATE SQL
     *
     * @param bean Entity对象
     *
     * @return String
     */
    public String getUpdatePrepareSQL(T bean) {
        if (this.tableStrategy == null) return updatePrepareSQL;
        return updatePrepareSQL.replace("${newtable}", getTable(bean));
    }

    /**
     * 获取Entity的UPDATE SQL
     *
     * @param bean Entity对象
     *
     * @return String
     */
    public String getUpdateDollarPrepareSQL(T bean) {
        if (this.tableStrategy == null) return updateDollarPrepareSQL;
        return updateDollarPrepareSQL.replace("${newtable}", getTable(bean));
    }

    /**
     * 获取Entity的UPDATE SQL
     *
     * @param bean Entity对象
     *
     * @return String
     */
    public String getUpdateNamesPrepareSQL(T bean) {
        if (this.tableStrategy == null) return updateNamesPrepareSQL;
        return updateNamesPrepareSQL.replace("${newtable}", getTable(bean));
    }

    /**
     * 获取Entity的DELETE SQL
     *
     * @param bean Entity对象
     *
     * @return String
     */
    public String getDeletePrepareSQL(T bean) {
        if (this.tableStrategy == null) return deletePrepareSQL;
        return deletePrepareSQL.replace("${newtable}", getTable(bean));
    }

    /**
     * 获取Entity的DELETE SQL
     *
     * @param bean Entity对象
     *
     * @return String
     */
    public String getDeleteDollarPrepareSQL(T bean) {
        if (this.tableStrategy == null) return deleteDollarPrepareSQL;
        return deleteDollarPrepareSQL.replace("${newtable}", getTable(bean));
    }

    /**
     * 获取Entity的DELETE SQL
     *
     * @param bean Entity对象
     *
     * @return String
     */
    public String getDeleteNamesPrepareSQL(T bean) {
        if (this.tableStrategy == null) return deleteNamesPrepareSQL;
        return deleteNamesPrepareSQL.replace("${newtable}", getTable(bean));
    }

    /**
     * 获取查询字段列表
     *
     * @param tabalis 表别名
     * @param selects 过滤字段
     *
     * @return String
     */
    public CharSequence getQueryColumns(String tabalis, SelectColumn selects) {
        if (selects == null) return tabalis == null ? "*" : (tabalis + ".*");
        StringBuilder sb = new StringBuilder();
        for (Attribute attr : this.attributes) {
            if (!selects.test(attr.field())) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(getSQLColumn(tabalis, attr.field()));
        }
        if (sb.length() == 0) sb.append('*');
        return sb;
    }

    public String getTableCopySQL(String newTable) {
        return tablecopySQL.replace("${newtable}", newTable).replace("${oldtable}", table);
    }

    /**
     * 根据主键值获取Entity的表名
     *
     * @param primary Entity主键值
     *
     * @return String
     */
    public String getTable(Serializable primary) {
        if (tableStrategy == null) return table;
        String t = tableStrategy.getTable(table, primary);
        return t == null || t.isEmpty() ? table : t;
    }

    /**
     * 根据过滤条件获取Entity的表名
     *
     * @param node 过滤条件
     *
     * @return String
     */
    public String getTable(FilterNode node) {
        if (tableStrategy == null) return table;
        String t = tableStrategy.getTable(table, node);
        return t == null || t.isEmpty() ? table : t;
    }

    /**
     * 根据Entity对象获取Entity的表名
     *
     * @param bean Entity对象
     *
     * @return String
     */
    public String getTable(T bean) {
        if (tableStrategy == null) return table;
        String t = tableStrategy.getTable(table, bean);
        return t == null || t.isEmpty() ? table : t;
    }

    /**
     * 获取主键字段的Attribute
     *
     * @return Attribute
     */
    public Attribute<T, Serializable> getPrimary() {
        return this.primary;
    }

    /**
     * 遍历数据库表对应的所有字段, 不包含&#64;Transient字段
     *
     * @param action BiConsumer
     */
    public void forEachAttribute(BiConsumer<String, Attribute<T, Serializable>> action) {
        this.attributeMap.forEach(action);
    }

    /**
     * 根据Entity字段名获取字段的Attribute
     *
     * @param fieldname Class字段名
     *
     * @return Attribute
     */
    public Attribute<T, Serializable> getAttribute(String fieldname) {
        if (fieldname == null) return null;
        return this.attributeMap.get(fieldname);
    }

    /**
     * 根据Entity字段名获取可更新字段的Attribute
     *
     * @param fieldname Class字段名
     *
     * @return Attribute
     */
    public Attribute<T, Serializable> getUpdateAttribute(String fieldname) {
        return this.updateAttributeMap.get(fieldname);
    }

    /**
     * 判断Entity类的字段名与表字段名s是否存在不一致的值
     *
     * @return boolean
     */
    public boolean isNoAlias() {
        return this.aliasmap == null;
    }

    /**
     * 根据Flipper获取ORDER BY的SQL语句，不存在Flipper或sort字段返回空字符串
     *
     * @param flipper 翻页对象
     *
     * @return String
     */
    protected String createSQLOrderby(Flipper flipper) {
        if (flipper == null || flipper.getSort() == null) return "";
        final String sort = flipper.getSort();
        if (sort.isEmpty() || sort.indexOf(';') >= 0 || sort.indexOf('\n') >= 0) return "";
        String sql = this.sortOrderbySqls.get(sort);
        if (sql != null) return sql;
        final StringBuilder sb = new StringBuilder();
        sb.append(" ORDER BY ");
        if (isNoAlias()) {
            sb.append(sort);
        } else {
            boolean flag = false;
            for (String item : sort.split(",")) {
                if (item.isEmpty()) continue;
                String[] sub = item.split("\\s+");
                if (flag) sb.append(',');
                if (sub.length < 2 || sub[1].equalsIgnoreCase("ASC")) {
                    sb.append(getSQLColumn("a", sub[0])).append(" ASC");
                } else {
                    sb.append(getSQLColumn("a", sub[0])).append(" DESC");
                }
                flag = true;
            }
        }
        sql = sb.toString();
        this.sortOrderbySqls.put(sort, sql);
        return sql;
    }

    /**
     * 根据field字段名获取数据库对应的字段名
     *
     * @param tabalis   表别名
     * @param fieldname 字段名
     *
     * @return String
     */
    public String getSQLColumn(String tabalis, String fieldname) {
        return this.aliasmap == null ? (tabalis == null ? fieldname : (tabalis + '.' + fieldname))
            : (tabalis == null ? aliasmap.getOrDefault(fieldname, fieldname) : (tabalis + '.' + aliasmap.getOrDefault(fieldname, fieldname)));
    }

    /**
     * 字段值转换成数据库的值
     *
     * @param fieldname  字段名
     * @param fieldvalue 字段值
     *
     * @return Object
     */
    public Object getSQLValue(String fieldname, Serializable fieldvalue) {
        if (this.cryptmap == null) return fieldvalue;
        CryptHandler handler = this.cryptmap.get(fieldname);
        if (handler == null) return fieldvalue;
        return handler.encrypt(fieldvalue);
    }

    /**
     * 字段值转换成带转义的数据库的值
     *
     * @param fieldname    字段名
     * @param fieldvalue   字段值
     * @param sqlFormatter 转义器
     *
     * @return CharSequence
     */
    public CharSequence formatSQLValue(String fieldname, Serializable fieldvalue, BiFunction<EntityInfo, Object, CharSequence> sqlFormatter) {
        Object val = getSQLValue(fieldname, fieldvalue);
        return sqlFormatter == null ? formatToString(val) : sqlFormatter.apply(this, val);
    }

    /**
     * 字段值转换成带转义的数据库的值
     *
     * @param value        字段值
     * @param sqlFormatter 转义器
     *
     * @return CharSequence
     */
    public CharSequence formatSQLValue(Object value, BiFunction<EntityInfo, Object, CharSequence> sqlFormatter) {
        return sqlFormatter == null ? formatToString(value) : sqlFormatter.apply(this, value);
    }

    /**
     * 字段值转换成数据库的值
     *
     * @param <F>    泛型
     * @param attr   Attribute
     * @param entity 记录对象
     *
     * @return Object
     */
    public <F> Object getSQLValue(Attribute<T, F> attr, T entity) {
        Object val = attr.get(entity);
        CryptHandler cryptHandler = attr.attach();
        if (cryptHandler != null) val = cryptHandler.encrypt(val);
        return val;
    }

    /**
     * 字段值转换成带转义的数据库的值
     *
     * @param <F>          泛型
     * @param attr         Attribute
     * @param entity       记录对象
     * @param sqlFormatter 转义器
     *
     * @return CharSequence
     */
    public <F> CharSequence formatSQLValue(Attribute<T, F> attr, T entity, BiFunction<EntityInfo, Object, CharSequence> sqlFormatter) {
        Object val = getSQLValue(attr, entity);
        return sqlFormatter == null ? formatToString(val) : sqlFormatter.apply(this, val);
    }

    /**
     * 数据库的值转换成数字段值
     *
     * @param attr   Attribute
     * @param entity 记录对象
     *
     * @return Object
     */
    public Serializable getFieldValue(Attribute<T, Serializable> attr, T entity) {
        Serializable val = attr.get(entity);
        CryptHandler cryptHandler = attr.attach();
        if (cryptHandler != null) val = (Serializable) cryptHandler.decrypt(val);
        return val;
    }

    /**
     * 获取主键字段的表字段名
     *
     * @return String
     */
    public String getPrimarySQLColumn() {
        return getSQLColumn(null, this.primary.field());
    }

    /**
     * 获取主键字段的带有表别名的表字段名
     *
     * @param tabalis 表别名
     *
     * @return String
     */
    public String getPrimarySQLColumn(String tabalis) {
        return getSQLColumn(tabalis, this.primary.field());
    }

    /**
     * 拼接UPDATE给字段赋值的SQL片段
     *
     * @param sqlColumn 表字段名
     * @param attr      Attribute
     * @param cv        ColumnValue
     * @param formatter 转义器
     *
     * @return CharSequence
     */
    protected CharSequence formatSQLValue(String sqlColumn, Attribute<T, Serializable> attr, final ColumnValue cv, BiFunction<EntityInfo, Object, CharSequence> formatter) {
        if (cv == null) return null;
        Object val = cv.getValue();
        //ColumnNodeValue时 cv.getExpress() == ColumnExpress.MOV 只用于updateColumn
        if (val instanceof ColumnNodeValue) return formatSQLValue(attr, null, (ColumnNodeValue) val, formatter);
        if (val instanceof ColumnFuncNode) return formatSQLValue(attr, null, (ColumnFuncNode) val, formatter);
        switch (cv.getExpress()) {
            case INC:
                return new StringBuilder().append(sqlColumn).append(" + ").append(val);
            case DEC:
                return new StringBuilder().append(sqlColumn).append(" - ").append(val);
            case MUL:
                return new StringBuilder().append(sqlColumn).append(" * ").append(val);
            case DIV:
                return new StringBuilder().append(sqlColumn).append(" / ").append(val);
            case MOD:
                return new StringBuilder().append(sqlColumn).append(" % ").append(val);
            case AND:
                return new StringBuilder().append(sqlColumn).append(" & ").append(val);
            case ORR:
                return new StringBuilder().append(sqlColumn).append(" | ").append(val);
            case MOV:
                CryptHandler handler = attr.attach();
                if (handler != null) val = handler.encrypt(val);
                return formatter == null ? formatToString(val) : formatter.apply(this, val);
        }
        CryptHandler handler = attr.attach();
        if (handler != null) val = handler.encrypt(val);
        return formatter == null ? formatToString(val) : formatter.apply(this, val);
    }

    protected CharSequence formatSQLValue(Attribute<T, Serializable> attr, String tabalis, final ColumnFuncNode node, BiFunction<EntityInfo, Object, CharSequence> formatter) {
        if (node.getValue() instanceof ColumnNodeValue) {
            return node.getFunc().getColumn(formatSQLValue(attr, tabalis, (ColumnNodeValue) node.getValue(), formatter).toString());
        } else {
            return node.getFunc().getColumn(this.getSQLColumn(tabalis, String.valueOf(node.getValue())));
        }
    }

    protected CharSequence formatSQLValue(Attribute<T, Serializable> attr, String tabalis, final ColumnNodeValue node, BiFunction<EntityInfo, Object, CharSequence> formatter) {
        Serializable left = node.getLeft();
        if (left instanceof CharSequence) {
            left = this.getSQLColumn(tabalis, left.toString());
        } else if (left instanceof ColumnNodeValue) {
            left = "(" + formatSQLValue(attr, tabalis, (ColumnNodeValue) left, formatter) + ")";
        } else if (left instanceof ColumnFuncNode) {
            left = "(" + formatSQLValue(attr, tabalis, (ColumnFuncNode) left, formatter) + ")";
        }
        Serializable right = node.getRight();
        if (right instanceof CharSequence) {
            right = this.getSQLColumn(null, right.toString());
        } else if (left instanceof ColumnNodeValue) {
            right = "(" + formatSQLValue(attr, tabalis, (ColumnNodeValue) right, formatter) + ")";
        } else if (left instanceof ColumnFuncNode) {
            right = "(" + formatSQLValue(attr, tabalis, (ColumnFuncNode) right, formatter) + ")";
        }
        switch (node.getExpress()) {
            case INC:
                return new StringBuilder().append(left).append(" + ").append(right);
            case DEC:
                return new StringBuilder().append(left).append(" - ").append(right);
            case MUL:
                return new StringBuilder().append(left).append(" * ").append(right);
            case DIV:
                return new StringBuilder().append(left).append(" / ").append(right);
            case MOD:
                return new StringBuilder().append(left).append(" % ").append(right);
            case AND:
                return new StringBuilder().append(left).append(" & ").append(right);
            case ORR:
                return new StringBuilder().append(left).append(" | ").append(right);
        }
        throw new IllegalArgumentException(node + " express cannot be null or MOV");
    }

    /**
     * 获取所有数据表字段的Attribute, 不包含&#64;Transient字段
     *
     * @return Map
     */
    protected Map<String, Attribute<T, Serializable>> getAttributes() {
        return attributeMap;
    }

    /**
     * 判断日志级别
     *
     * @param logger Logger
     * @param l      Level
     *
     * @return boolean
     */
    public boolean isLoggable(Logger logger, Level l) {
        return logger.isLoggable(l) && l.intValue() >= this.logLevel;
    }

    public boolean isNotNullable(String fieldname) {
        return notNullColumns.contains(fieldname);
    }

    public boolean isNotNullable(Attribute<T, Serializable> attr) {
        return attr == null ? false : notNullColumns.contains(attr.field());
    }

    /**
     * 判断日志级别
     *
     * @param logger Logger
     * @param l      Level
     * @param str    String
     *
     * @return boolean
     */
    public boolean isLoggable(Logger logger, Level l, String str) {
        boolean rs = logger.isLoggable(l) && l.intValue() >= this.logLevel;
        if (this.excludeLogLevels == null || !rs || str == null) return rs;
        String[] keys = this.excludeLogLevels.get(l.intValue());
        if (keys == null) return rs;
        for (String key : keys) {
            if (str.contains(key)) return false;
        }
        return rs;
    }

    /**
     * 将字段值序列化为可SQL的字符串
     *
     * @param value 字段值
     *
     * @return String
     */
    private String formatToString(Object value) {
        if (value == null) return null;
        if (value instanceof CharSequence) {
            return new StringBuilder().append('\'').append(value.toString().replace("'", "\\'")).append('\'').toString();
        } else if (!(value instanceof Number) && !(value instanceof java.util.Date)
            && !value.getClass().getName().startsWith("java.sql.") && !value.getClass().getName().startsWith("java.time.")) {
            return new StringBuilder().append('\'').append(jsonConvert.convertTo(value).replace("'", "\\'")).append('\'').toString();
        }
        return String.valueOf(value);
    }

    /**
     * 将一行的ResultSet组装成一个Entity对象
     *
     * @param sels 指定字段
     * @param set  ResultSet
     *
     * @return Entity对象
     * @throws SQLException SQLException
     */
    protected T getEntityValue(final SelectColumn sels, final ResultSet set) throws SQLException {
        T obj;
        Attribute<T, Serializable>[] attrs = this.queryAttributes;
        if (this.constructorParameters == null) {
            obj = creator.create();
        } else {
            Object[] cps = new Object[this.constructorParameters.length];
            for (int i = 0; i < this.constructorAttributes.length; i++) {
                Attribute<T, Serializable> attr = this.constructorAttributes[i];
                if (sels == null || sels.test(attr.field())) {
                    cps[i] = getFieldValue(attr, set);
                }
            }
            obj = creator.create(cps);
            attrs = this.unconstructorAttributes;
        }
        for (Attribute<T, Serializable> attr : attrs) {
            if (sels == null || sels.test(attr.field())) {
                attr.set(obj, getFieldValue(attr, set));
            }
        }
        return obj;
    }

    protected Serializable getFieldValue(Attribute<T, Serializable> attr, final ResultSet set) throws SQLException {
        return getFieldValue(attr, set, 0);
    }

    protected Serializable getFieldValue(Attribute<T, Serializable> attr, final ResultSet set, int index) throws SQLException {
        final Class t = attr.type();
        Serializable o;
        if (t == byte[].class) {
            Blob blob = index > 0 ? set.getBlob(index) : set.getBlob(this.getSQLColumn(null, attr.field()));
            if (blob == null) {
                o = null;
            } else { //不支持超过2G的数据
                o = blob.getBytes(1, (int) blob.length());
                CryptHandler cryptHandler = attr.attach();
                if (cryptHandler != null) o = (Serializable) cryptHandler.decrypt(o);
            }
        } else {
            o = (Serializable) (index > 0 ? set.getObject(index) : set.getObject(this.getSQLColumn(null, attr.field())));
            CryptHandler cryptHandler = attr.attach();
            if (cryptHandler != null) o = (Serializable) cryptHandler.decrypt(o);
            if (t.isPrimitive()) {
                if (o != null) {
                    if (t == int.class) {
                        o = ((Number) o).intValue();
                    } else if (t == long.class) {
                        o = ((Number) o).longValue();
                    } else if (t == short.class) {
                        o = ((Number) o).shortValue();
                    } else if (t == float.class) {
                        o = ((Number) o).floatValue();
                    } else if (t == double.class) {
                        o = ((Number) o).doubleValue();
                    } else if (t == byte.class) {
                        o = ((Number) o).byteValue();
                    } else if (t == char.class) {
                        o = (char) ((Number) o).intValue();
                    } else if (t == boolean.class) {
                        o = (Boolean) o;
                    }
                } else if (t == int.class) {
                    o = 0;
                } else if (t == long.class) {
                    o = 0L;
                } else if (t == short.class) {
                    o = (short) 0;
                } else if (t == float.class) {
                    o = 0.0f;
                } else if (t == double.class) {
                    o = 0.0d;
                } else if (t == byte.class) {
                    o = (byte) 0;
                } else if (t == boolean.class) {
                    o = false;
                } else if (t == char.class) {
                    o = (char) 0;
                }
            } else if (t == AtomicInteger.class) {
                if (o != null) {
                    o = new AtomicInteger(((Number) o).intValue());
                } else {
                    o = new AtomicInteger();
                }
            } else if (t == AtomicLong.class) {
                if (o != null) {
                    o = new AtomicLong(((Number) o).longValue());
                } else {
                    o = new AtomicLong();
                }
            } else if (o != null && !t.isAssignableFrom(o.getClass()) && o instanceof CharSequence) {
                o = ((CharSequence) o).length() == 0 ? null : jsonConvert.convertFrom(attr.genericType(), o.toString());
            }
        }
        return o;
    }
}
