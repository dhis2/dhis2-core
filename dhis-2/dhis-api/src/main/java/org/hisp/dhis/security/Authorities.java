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
package org.hisp.dhis.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 */
public enum Authorities {
  ALL,
  F_VIEW_EVENT_ANALYTICS,
  F_METADATA_EXPORT,
  F_METADATA_IMPORT,
  F_EXPORT_DATA,
  F_SKIP_DATA_IMPORT_AUDIT,
  F_APPROVE_DATA,
  F_APPROVE_DATA_LOWER_LEVELS,
  F_ACCEPT_DATA_LOWER_LEVELS,
  F_PERFORM_MAINTENANCE,
  F_PERFORM_ANALYTICS_EXPLAIN,
  F_LOCALE_ADD,
  F_GENERATE_MIN_MAX_VALUES,
  F_RUN_VALIDATION,
  F_PREDICTOR_RUN,
  F_SEND_EMAIL,
  F_ORGANISATIONUNIT_MOVE,
  F_ORGANISATION_UNIT_SPLIT,
  F_ORGANISATION_UNIT_MERGE,
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
  F_IGNORE_TRACKER_REQUIRED_VALUE_VALIDATION,
  F_VIEW_SERVER_INFO,
  F_ORG_UNIT_PROFILE_ADD,
  F_TRACKED_ENTITY_MERGE,
  F_DATAVALUE_ADD,
  F_IMPERSONATE_USER,
  F_SYSTEM_SETTING;

  public static Set<String> getAllAuthorities() {
    return Arrays.stream(Authorities.values()).map(Authorities::name).collect(Collectors.toSet());
  }
}
