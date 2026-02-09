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
package org.hisp.dhis.tracker.export;

import java.io.IOException;
import javax.annotation.CheckForNull;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.hibernate.jsonb.type.JsonBinaryType;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.program.UserInfoSnapshot;

public class UserInfoSnapshots {
  private UserInfoSnapshots() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Parses JSON into a UserInfoSnapshot.
   *
   * @param json JSON string representing a UserInfoSnapshot
   * @return parsed UserInfoSnapshot, or null if input is empty
   */
  @CheckForNull
  public static UserInfoSnapshot fromJson(String json) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    try {
      return JsonBinaryType.MAPPER.readValue(json, UserInfoSnapshot.class);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Creates a UserInfoSnapshot from a JsonObject (lazy JSON tree).
   *
   * @param json JsonObject representing a UserInfoSnapshot
   * @return UserInfoSnapshot, or null if input is null or undefined
   */
  @CheckForNull
  public static UserInfoSnapshot from(@CheckForNull JsonObject json) {
    if (json == null || json.isUndefined()) {
      return null;
    }
    return UserInfoSnapshot.of(
        json.getNumber("id").number().longValue(),
        json.getString("code").string(null),
        json.getString("uid").string(null),
        json.getString("username").string(null),
        json.getString("firstName").string(null),
        json.getString("surname").string(null));
  }
}
