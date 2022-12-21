/** *****************************************************************************
 * Copyright (c) 2008 - 2013 Oracle Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Linda DeMichiel - Java Persistence 2.1
 *     Linda DeMichiel - Java Persistence 2.0
 *
 ***************************************************************************** */
package org.redkale.persistence;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the mapped column for a persistent property or field.
 * If no <code>Column</code> annotation is specified, the default values apply.
 *
 * <blockquote><pre>
 *    Example 1:
 *
 *    &#064;Column(name="DESC", nullable=false, length=512)
 *    public String getDescription() { return description; }
 *
 *    Example 2:
 *
 *    &#064;Column(name="DESC",
 *            columnDefinition="CLOB NOT NULL",
 *            table="EMP_DETAIL")
 *    &#064;Lob
 *    public String getDescription() { return description; }
 *
 *    Example 3:
 *
 *    &#064;Column(name="ORDER_COST", updatable=false, precision=12, scale=2)
 *    public BigDecimal getCost() { return cost; }
 *
 * </pre></blockquote>
 *
 *
 * @since Java Persistence 1.0
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Column {

    /**
     * (Optional) The name of the column. Defaults to
     * the property or field name.
     *
     * @return String
     */
    String name() default "";

    /**
     * (Optional) The comment of the column.
     *
     * @return String
     */
    String comment() default "";

    /**
     * (Optional) Whether the column is a unique key. This is a
     * shortcut for the <code>UniqueConstraint</code> annotation at the table
     * level and is useful for when the unique key constraint
     * corresponds to only a single column. This constraint applies
     * in addition to any constraint entailed by primary key mapping and
     * to constraints specified at the table level.
     *
     * @return boolean
     */
    boolean unique() default false;

    /**
     * (Optional) Whether the database column is required.
     *
     * @return boolean
     */
    boolean nullable() default true;

    /**
     * for OpenAPI Specification 3
     *
     * @return String
     */
    String example() default "";

    /**
     * (Optional) Whether the column is included in SQL INSERT
     * statements generated by the persistence provider.
     *
     * @return boolean
     */
    boolean insertable() default true;

    /**
     * (Optional) Whether the column is included in SQL UPDATE
     * statements generated by the persistence provider.
     *
     * @return boolean
     */
    boolean updatable() default true;

    /**
     * (Optional) The name of the table that contains the column.
     * If absent the column is assumed to be in the primary table.
     *
     * @return String
     */
    @Deprecated
    String table() default "";

    /**
     * (Optional) The column length. (Applies only if a
     * string-valued column is used.)
     * if type==String and length == 65535 then sqltype is TEXT   <br>
     * if type==String and length &#60;= 16777215 then sqltype is MEDIUMTEXT   <br>
     * if type==String and length &#62; 16777215 then sqltype is LONGTEXT   <br>
     * if type==byte[] and length &#60;= 65535 then sqltype is BLOB   <br>
     * if type==byte[] and length &#60;= 16777215 then sqltype is MEDIUMBLOB   <br>
     * if type==byte[] and length &#62; 16777215 then sqltype is LONGBLOB   <br>
     *
     * @return int
     */
    int length() default 255;

    /**
     * (Optional) The precision for a decimal (exact numeric)
     * column. (Applies only if a decimal column is used.)
     * Value must be set by developer if used when generating
     * the DDL for the column.
     *
     * @return int
     */
    int precision() default 0;

    /**
     * (Optional) The scale for a decimal (exact numeric) column.
     * (Applies only if a decimal column is used.)
     *
     * @return int
     */
    int scale() default 0;
}
