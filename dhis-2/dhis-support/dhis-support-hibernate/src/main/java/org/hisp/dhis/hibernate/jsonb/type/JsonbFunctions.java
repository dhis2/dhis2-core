/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.hibernate.jsonb.type;

/**
 * Names of custom JSONB function created in database Those functions can be called in JPA Criteria
 * query using builder.function() All functions must be registered in {@link
 * org.hisp.dhis.hibernate.dialect.DhisPostgresDialect}
 */
public class JsonbFunctions {
  /**
   * FUNCTION jsonb_has_user_group_ids(jsonb, text) $1: Sharing jsonb column $2: Array of UserGroup
   * uid
   *
   * @return True if the given jsonb has at least one of UserGroup's uid from the given array
   */
  public static final String HAS_USER_GROUP_IDS = "jsonb_has_user_group_ids";

  /**
   * FUNCTION jsonb_check_user_groups_access(jsonb, text, text) $1: Sharing jsonb column $2: Access
   * String to check $3: Array of UserGroup uid
   *
   * @return True if the given jsonb has at least one of UserGroup's uid from the given array and
   *     the access of UserGroup `like` given access
   */
  public static final String CHECK_USER_GROUPS_ACCESS = "jsonb_check_user_groups_access";

  /**
   * FUNCTION jsonb_has_user_id(jsonb, text ) $1: Sharing jsonb column $2: User uid to check
   *
   * @return True if given jsonb has user uid
   */
  public static final String HAS_USER_ID = "jsonb_has_user_id";

  /**
   * FUNCTION jsonb_check_user_access(jsonb, text, text) $1: Sharing jsonb column $2: User uid to
   * check $3: Access string to check
   *
   * @return TRUE if given jsonb has user uid and user access like given access string
   */
  public static final String CHECK_USER_ACCESS = "jsonb_check_user_access";

  /** Built-in function of PostgresQL */
  public static final String EXTRACT_PATH = "jsonb_extract_path";

  /** Built-in function of PostgresQL */
  public static final String EXTRACT_PATH_TEXT = "jsonb_extract_path_text";

  /** Built-in function of PostgresQL */
  public static final String JSONB_TYPEOF = "jsonb_typeof";

  /**
   * Use the regex operator '~*' to match a given string with a given regular expression $1 String
   * to search $2 Regular expression for matching
   */
  public static final String REGEXP_SEARCH = "regexp_search";
}
