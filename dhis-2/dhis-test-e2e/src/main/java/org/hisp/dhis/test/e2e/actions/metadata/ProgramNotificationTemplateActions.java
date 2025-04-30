/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.test.e2e.actions.metadata;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.JsonObjectBuilder;

/**
 * @author Zubair Asghar
 */
public class ProgramNotificationTemplateActions extends RestApiActions {
  public ProgramNotificationTemplateActions() {
    super("/programNotificationTemplates");
  }

  /**
   * Create ProgramNotificationTemplate and return its id
   *
   * @return ProgramNotificationTemplate id
   */
  public String createProgramNotificationTemplate() {
    JsonArray deliveryChannels = new JsonArray();
    deliveryChannels.add("SMS");
    JsonObject pnt =
        new JsonObjectBuilder()
            .addProperty("name", "test_template")
            .addProperty("code", "test_template")
            .addProperty("messageTemplate", "message text")
            .addProperty("subjectTemplate", "subject text")
            .addProperty("notificationTrigger", "COMPLETION")
            .addProperty("notificationRecipient", "USER_GROUP")
            .addArray("deliveryChannels", deliveryChannels)
            .build();

    ApiResponse response = this.post(pnt);

    response.validate().statusCode(is(oneOf(201, 200)));
    return response.extractUid();
  }
}
