/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.utils;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.MapUtils;

/** Utility methods for creation and update Sharing object. */
public class SharingUtils {
  public static JsonObject createSharingObject(String publicAccess) {
    return createSharingObject(null, publicAccess, Map.of(), Map.of());
  }

  public static JsonObject createSharingObject(
      String owner,
      String publicAccess,
      Map<String, String> users,
      Map<String, String> userGroups) {
    JsonObject sharing = new JsonObject();

    if (publicAccess != null) {
      sharing.addProperty("public", publicAccess);
    }

    if (owner != null) {
      sharing.addProperty("owner", owner);
    }

    sharing.addProperty("external", false);

    if (!MapUtils.isEmpty(userGroups)) {
      JsonObject userGroupObject = new JsonObject();
      userGroups
          .keySet()
          .forEach(uid -> userGroupObject.add(uid, createAccessObject(uid, userGroups.get(uid))));
      sharing.add("userGroups", userGroupObject);
    }

    if (!MapUtils.isEmpty(users)) {
      JsonObject userObject = new JsonObject();
      users.keySet().forEach(uid -> userObject.add(uid, createAccessObject(uid, users.get(uid))));
      sharing.add("users", userObject);
    }

    return sharing;
  }

  public static JsonObject addUserGroupAccess(
      JsonObject sharingObject, String uid, String accessString) {
    JsonObject userGroupAccess = sharingObject.getAsJsonObject("userGroups");

    if (userGroupAccess == null) {
      userGroupAccess = new JsonObject();
      sharingObject.add("userGroups", userGroupAccess);
    }

    userGroupAccess.add(uid, createAccessObject(uid, accessString));
    return sharingObject;
  }

  public static JsonObject addUserAccess(
      JsonObject sharingObject, String uid, String accessString) {
    JsonObject userAccess = sharingObject.getAsJsonObject("users");

    if (userAccess == null) {
      sharingObject.add("users", new JsonObject());
      userAccess = sharingObject.getAsJsonObject("users");
    }

    userAccess.add(uid, createAccessObject(uid, accessString));
    return sharingObject;
  }

  public static JsonObject createAccessObject(String uid, String accessString) {
    JsonObject access = new JsonObject();
    access.addProperty("id", uid);
    access.addProperty("access", accessString);

    return access;
  }

  public static String getSafe(JsonObject object, String key) {
    if (!object.has("sharing")) {
      return null;
    }

    JsonObject sharingObject = object.getAsJsonObject("sharing");

    return sharingObject.has(key) ? sharingObject.get(key).getAsString() : null;
  }

  public static Map<String, String> getAccessObjects(JsonObject object, String key) {
    if (!object.has("sharing")) {
      return null;
    }

    JsonObject sharingObject = object.getAsJsonObject("sharing");

    if (!sharingObject.has(key)) {
      return null;
    }

    JsonObject accessObject = sharingObject.getAsJsonObject(key);

    return accessObject.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().getAsJsonObject().get("access").getAsString()));
  }
}
