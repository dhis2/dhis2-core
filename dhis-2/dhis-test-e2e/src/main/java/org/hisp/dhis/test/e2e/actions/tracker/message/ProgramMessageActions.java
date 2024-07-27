/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.test.e2e.actions.tracker.message;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.hisp.dhis.test.e2e.TestRunStorage;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.JsonObjectBuilder;

/**
 * @author Zubair Asghar
 */
public class ProgramMessageActions extends RestApiActions {

  public ProgramMessageActions() {
    super("/messages");
  }

  /**
   * Create ProgramMessage and return its id
   *
   * @return ProgramMessage id
   */
  public String sendProgramMessage(String enrollment, String orgUnit) {
    String uid = "U5HE4IRrZ32";
    JsonArray deliveryChannels = new JsonArray();
    deliveryChannels.add("SMS");

    JsonObject programMessage =
        new JsonObjectBuilder()
            .addProperty("name", "test_program_message")
            .addProperty("id", "U5HE4IRrZ32")
            .addProperty("text", "message text")
            .addProperty("subject", "subject text")
            .addProperty("messageStatus", "SENT")
            .addProperty("processedDate", Instant.now().plus(1, ChronoUnit.DAYS).toString())
            .addArray("deliveryChannels", deliveryChannels)
            .addObject(
                "recipients",
                JsonObjectBuilder.jsonObject()
                    .addObject(
                        "organisationUnit",
                        JsonObjectBuilder.jsonObject().addProperty("id", orgUnit)))
            .addObject("enrollment", JsonObjectBuilder.jsonObject().addProperty("id", enrollment))
            .build();

    JsonArray programMessageList = new JsonArray();
    programMessageList.add(programMessage);

    ApiResponse response =
        this.post(
            JsonObjectBuilder.jsonObject().addArray("programMessages", programMessageList).build());

    response.validate().statusCode(is(oneOf(201, 200)));
    TestRunStorage.addCreatedEntity(endpoint, uid);

    return response.extractUid();
  }
}
