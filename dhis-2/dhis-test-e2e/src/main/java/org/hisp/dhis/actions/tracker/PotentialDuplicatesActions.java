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
package org.hisp.dhis.actions.tracker;

import com.google.gson.JsonObject;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class PotentialDuplicatesActions extends RestApiActions {
  public PotentialDuplicatesActions() {
    super("/potentialDuplicates");
  }

  public String createAndValidatePotentialDuplicate(String teiA, String teiB, String status) {
    JsonObject object =
        new JsonObjectBuilder()
            .addProperty("original", teiA)
            .addProperty("duplicate", teiB)
            .addProperty("status", "OPEN")
            .build();

    String uid = this.post(object).validateStatus(200).extractUid();

    if (status.equals("MERGED")) {
      this.autoMergePotentialDuplicate(uid).validateStatus(200);
    }

    if (status.equals("INVALID")) {
      this.update(uid + "?status=INVALID", new JsonObjectBuilder().build()).validateStatus(200);
    }

    return uid;
  }

  public ApiResponse postPotentialDuplicate(String teiA, String teiB, String status) {
    JsonObject object =
        new JsonObjectBuilder()
            .addProperty("original", teiA)
            .addProperty("duplicate", teiB)
            .addProperty("status", status)
            .build();

    return this.post(object);
  }

  public ApiResponse manualMergePotentialDuplicate(
      String potentialDuplicate, JsonObject jsonObject) {
    return this.post(
        String.format("/%s/merge?mergeStrategy=MANUAL", potentialDuplicate), jsonObject);
  }

  public ApiResponse autoMergePotentialDuplicate(String potentialDuplicate) {
    return this.post(
        String.format("/%s/merge?mergeStrategy=AUTO", potentialDuplicate), new JsonObject());
  }
}
