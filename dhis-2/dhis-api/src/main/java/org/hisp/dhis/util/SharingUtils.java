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
package org.hisp.dhis.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;
import java.util.function.UnaryOperator;
import org.apache.commons.collections4.MapUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;

public class SharingUtils {
  private static final ImmutableList<String> LEGACY_SHARING_PROPERTIES =
      ImmutableList.<String>builder()
          .add("userAccesses", "userGroupAccesses", "publicAccess", "externalAccess")
          .build();

  private static final ObjectMapper FROM_AND_TO_JSON = createMapper();

  private SharingUtils() {
    throw new UnsupportedOperationException("utility");
  }

  public static String withAccess(String jsonb, UnaryOperator<String> accessTransformation)
      throws JsonProcessingException {
    Sharing value = FROM_AND_TO_JSON.readValue(jsonb, Sharing.class);
    return FROM_AND_TO_JSON.writeValueAsString(value.withAccess(accessTransformation));
  }

  public static boolean isLegacySharingProperty(Property property) {
    return LEGACY_SHARING_PROPERTIES.contains(property.getFieldName());
  }

  private static ObjectMapper createMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    return mapper;
  }

  public static String sharingToString(BaseIdentifiableObject object, String currentUserName) {
    StringBuilder builder =
        new StringBuilder()
            .append("'")
            .append(currentUserName)
            .append("'")
            .append(" update sharing on ")
            .append(object.getClass().getName())
            .append(", uid: ")
            .append(object.getUid())
            .append(", name: ")
            .append(object.getName())
            .append(", publicAccess: ")
            .append(object.getSharing().getPublicAccess())
            .append(", externalAccess: ")
            .append(object.getSharing().isExternal());

    if (!MapUtils.isEmpty(object.getSharing().getUserGroups())) {
      builder.append(", userGroupAccesses: ");

      for (UserGroupAccess userGroupAccess : object.getSharing().getUserGroups().values()) {
        builder
            .append("{uid: ")
            .append(userGroupAccess.getId())
            .append(", name: ")
            .append(userGroupAccess.getDisplayName())
            .append(", access: ")
            .append(userGroupAccess.getAccess())
            .append("} ");
      }
    }

    if (!MapUtils.isEmpty(object.getSharing().getUsers())) {
      builder.append(", userAccesses: ");

      for (UserAccess userAccess : object.getSharing().getUsers().values()) {
        builder
            .append("{uid: ")
            .append(userAccess.getId())
            .append(", name: ")
            .append(userAccess.getDisplayName())
            .append(", access: ")
            .append(userAccess.getAccess())
            .append("} ");
      }
    }

    return builder.toString();
  }
}
