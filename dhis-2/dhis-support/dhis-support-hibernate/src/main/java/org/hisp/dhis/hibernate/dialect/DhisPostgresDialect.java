/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.hibernate.dialect;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import java.sql.Types;
import java.util.List;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spatial.dialect.postgis.PostgisPG95Dialect;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Stian Sandvold <stian@dhis2.org>
 */
public class DhisPostgresDialect extends PostgisPG95Dialect {
  public DhisPostgresDialect() {
    super();
    registerColumnType(Types.JAVA_OBJECT, "jsonb");
    registerHibernateType(Types.OTHER, "pg-uuid");
    registerHibernateType(Types.ARRAY, StringArrayType.class.getName());
    registerFunction(
        "jsonb_exists_any",
        new StandardSQLFunction("jsonb_exists_any", StandardBasicTypes.BOOLEAN));
    registerFunction(
        JsonbFunctions.EXTRACT_PATH,
        new StandardSQLFunction(JsonbFunctions.EXTRACT_PATH, StandardBasicTypes.STRING));
    registerFunction(
        JsonbFunctions.EXTRACT_PATH_TEXT,
        new StandardSQLFunction(JsonbFunctions.EXTRACT_PATH_TEXT, StandardBasicTypes.STRING));
    registerFunction(
        JsonbFunctions.JSONB_TYPEOF,
        new StandardSQLFunction(JsonbFunctions.JSONB_TYPEOF, StandardBasicTypes.STRING));
    registerFunction(
        JsonbFunctions.HAS_USER_GROUP_IDS,
        new StandardSQLFunction(JsonbFunctions.HAS_USER_GROUP_IDS, StandardBasicTypes.BOOLEAN));
    registerFunction(
        JsonbFunctions.CHECK_USER_GROUPS_ACCESS,
        new StandardSQLFunction(
            JsonbFunctions.CHECK_USER_GROUPS_ACCESS, StandardBasicTypes.BOOLEAN));
    registerFunction(
        JsonbFunctions.HAS_USER_ID,
        new StandardSQLFunction(JsonbFunctions.HAS_USER_ID, StandardBasicTypes.BOOLEAN));
    registerFunction(
        JsonbFunctions.CHECK_USER_ACCESS,
        new StandardSQLFunction(JsonbFunctions.CHECK_USER_ACCESS, StandardBasicTypes.BOOLEAN));
    registerFunction(
        JsonbFunctions.REGEXP_SEARCH,
        new StandardSQLFunction(JsonbFunctions.REGEXP_SEARCH, StandardBasicTypes.BOOLEAN));
    registerFunction(
        JsonbFunctions.SEARCH_TRANSLATION_TOKEN,
        new StandardSQLFunction(
            JsonbFunctions.SEARCH_TRANSLATION_TOKEN, StandardBasicTypes.BOOLEAN));
    registerFunction(
        JsonbFunctions.GET_TRANSLATED_VALUE,
        new StandardSQLFunction(JsonbFunctions.GET_TRANSLATED_VALUE, StandardBasicTypes.STRING));
    // Unlike translated ordering, display-name filtering must skip empty values and match the
    // getter's case-insensitive property key and conditional String.trim() fallback.
    registerFunction(
        JsonbFunctions.GET_DISPLAY_NAME,
        new StandardSQLFunction(JsonbFunctions.GET_DISPLAY_NAME, StandardBasicTypes.STRING) {
          @Override
          public String render(
              Type firstArgumentType, List arguments, SessionFactoryImplementor factory) {
            // Keep this scalar: an array-expansion subquery per candidate inflates both execution
            // cost and PostgreSQL's cost estimate, triggering expensive JIT for simple searches.
            // SQLFunctionTemplate cannot represent the JSONPath '?' operator.
            // Preserve argument order: Criteria binds the path before the locale.
            return """
                case when coalesce(jsonb_array_length(%1$s), 0) = 0 then %2$s
                else coalesce(
                  jsonb_path_query_first(
                    %1$s, cast(%3$s as jsonpath),
                    jsonb_build_object('locale', cast(%4$s as text))) ->> 'value',
                  regexp_replace(%2$s, '^[\\x01-\\x20]+|[\\x01-\\x20]+$', '', 'g'))
                end
                """
                .formatted(arguments.get(0), arguments.get(1), arguments.get(2), arguments.get(3));
          }
        });
    registerFunction("array_agg", new StandardSQLFunction("array_agg", StringArrayType.INSTANCE));
  }
}
