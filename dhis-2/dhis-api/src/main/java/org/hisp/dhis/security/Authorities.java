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
package org.hisp.dhis.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An enum of authorities. A {@link org.hisp.dhis.user.User} will usually have one or more of these.
 * We perform programmatic checking throughout the system with these values (usually in Controllers
 * or Services).
 *
 * <p>
 *
 * <p>
 *
 * <p>An Authority can be defined using the standard Java public static final constant format e.g.
 * THIS_IS_AN_AUTHORITY. When used in this format (no string passed in the constructor), the {@link
 * Authorities#name()} is used as the value when performing Authority checking in {@link
 * AuthorityInterceptor}.
 *
 * <p>
 *
 * <p>
 *
 * <p>An Authority can also be declared using the standard Java public static final constant format
 * along with a String value e.g. AUTHORITY_WITH_STRING("this value is used for auth checking").
 * When used in this format (string passed in the constructor), the string passed in the constructor
 * is used as the value when performing Authority checking in {@link AuthorityInterceptor}.
 *
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 * @author david mackessy
 */
public enum Authorities {
  ALL,
  F_CAPTURE_DATASTORE_UPDATE,
  F_VIEW_EVENT_ANALYTICS,
  F_METADATA_EXPORT,
  F_METADATA_IMPORT,
  F_METADATA_MANAGE,
  F_EXPORT_DATA,
  F_SKIP_DATA_IMPORT_AUDIT,
  F_APPROVE_DATA,
  F_APPROVE_DATA_LOWER_LEVELS,
  F_ACCEPT_DATA_LOWER_LEVELS,
  F_PERFORM_MAINTENANCE,
  F_PERFORM_ANALYTICS_EXPLAIN,
  F_LOCALE_ADD,
  F_LOCALE_DELETE,
  F_GENERATE_MIN_MAX_VALUES,
  F_MINMAX_DATAELEMENT_ADD,
  F_RUN_VALIDATION,
  F_PREDICTOR_RUN,
  F_SEND_EMAIL,
  F_ORGANISATIONUNIT_MOVE,
  F_ORGANISATION_UNIT_SPLIT,
  F_ORGANISATION_UNIT_MERGE,
  F_INDICATOR_TYPE_MERGE,
  F_INDICATOR_MERGE,
  F_DATA_ELEMENT_MERGE,
  F_CATEGORY_OPTION_MERGE,
  F_CATEGORY_OPTION_COMBO_MERGE,
  F_INSERT_CUSTOM_JS_CSS,
  F_VIEW_UNAPPROVED_DATA,
  F_USER_VIEW,
  F_REPLICATE_USER,
  F_USER_GROUPS_READ_ONLY_ADD_MEMBERS,
  F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS,
  F_TEI_CASCADE_DELETE,
  F_ENROLLMENT_CASCADE_DELETE,
  F_UNCOMPLETE_EVENT,
  F_EDIT_EXPIRED,
  F_VIEW_SERVER_INFO,
  F_ORG_UNIT_PROFILE_ADD,
  F_TRACKED_ENTITY_MERGE,
  F_DATAVALUE_ADD,
  F_IMPERSONATE_USER,
  F_SYSTEM_SETTING,
  F_LEGEND_SET_PUBLIC_ADD,
  F_LEGEND_SET_PRIVATE_ADD,
  F_LEGEND_SET_DELETE,
  F_MOBILE_SENDSMS,
  F_JOB_LOG_READ,
  F_MOBILE_SETTINGS,
  F_PREVIOUS_IMPERSONATOR_AUTHORITY,
  // bundled authority,
  M_DHIS_WEB_APP_MANAGEMENT("M_dhis-web-app-management");

  private final String authorityName;

  /**
   * Constructor with String param. The String passed in will be the value returned in {@link
   * Authorities#toString()}
   *
   * @param authority authority
   */
  Authorities(String authority) {
    this.authorityName = authority;
  }

  /**
   * Empty constructor. {@link Enum#name()} will be the value returned in {@link
   * Authorities#toString()}
   */
  Authorities() {
    this.authorityName = this.name();
  }

  public static Set<String> getAllAuthorities() {
    return Arrays.stream(Authorities.values())
        .map(Authorities::toString)
        .collect(Collectors.toSet());
  }

  /**
   * @return the string value held in {@link Authorities#authorityName}. The value held depends on
   *     how the enum value is defined (with or without a string in the constructor).
   */
  @Override
  public String toString() {
    return this.authorityName;
  }

  /**
   * Util method to transform Authorities[] to String[]
   *
   * @param authorities - authorities
   * @return String[] of transformed authorities
   */
  public static String[] toStringArray(Authorities... authorities) {
    if (authorities == null) {
      return new String[0];
    }
    return Arrays.stream(authorities).map(Authorities::toString).toList().toArray(new String[0]);
  }

  /**
   * Util method to transform {@link Collection}<{@link Authorities}> to its String alternative
   * {@link List}<{@link String}>
   *
   * @param authorities {@link Collection}<{@link Authorities}>
   * @return {@link List}<{@link String}> of transformed authorities to their string values
   */
  public static List<String> toStringList(Collection<Authorities> authorities) {
    if (authorities == null) {
      return List.of();
    }
    return authorities.stream().map(Authorities::toString).toList();
  }
}
