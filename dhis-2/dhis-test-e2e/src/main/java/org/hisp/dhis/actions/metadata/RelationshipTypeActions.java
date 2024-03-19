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
package org.hisp.dhis.actions.metadata;

import com.google.gson.JsonObject;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.utils.DataGenerator;
import org.hisp.dhis.utils.SharingUtils;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class RelationshipTypeActions extends RestApiActions {
  public RelationshipTypeActions() {
    super("relationshipTypes");
  }

  public String createRelationshipType(
      String fromEntityType,
      String fromConstraintId,
      String toEntityType,
      String toConstraintId,
      boolean bidirectional) {
    JsonObject object =
        new JsonObjectBuilder()
            .addObject("sharing", SharingUtils.createSharingObject("rwrw----"))
            .addProperty("name", "TA_RELATIONSHIP_TYPE " + DataGenerator.randomString())
            .addProperty("fromToName", "Test to")
            .addProperty("toFromName", "Test from")
            .addObject(
                "fromConstraint", createRelationshipConstraint(fromEntityType, fromConstraintId))
            .addObject("toConstraint", createRelationshipConstraint(toEntityType, toConstraintId))
            .addProperty("bidirectional", String.valueOf(bidirectional))
            .addUserGroupAccess()
            .build();

    return this.create(object);
  }

  private JsonObject createRelationshipConstraint(String type, String id) {
    JsonObjectBuilder builder = new JsonObjectBuilder().addProperty("relationshipEntity", type);
    switch (type) {
      case "PROGRAM_STAGE_INSTANCE":
        builder.addObject("programStage", new JsonObjectBuilder().addProperty("id", id));
        break;

      case "TRACKED_ENTITY_INSTANCE":
        builder.addObject("trackedEntityType", new JsonObjectBuilder().addProperty("id", id));
        break;
    }

    return builder.build();
  }
}
