package org.hisp.dhis.test.e2e.actions.tracker.message;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.jsontree.JsonBuilder;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.JsonObjectBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;

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
        JsonArray deliveryChannels = new JsonArray();
        deliveryChannels.add("SMS");

        JsonObject programMessage =
                new JsonObjectBuilder()
                        .addProperty("name", "test_program_message")
                        .addProperty("code", "test_program_message")
                        .addProperty("text", "message text")
                        .addProperty("subject", "subject text")
                        .addProperty("messageStatus", "SENT")
                        .addProperty("processedDate", Instant.now().plus(1, ChronoUnit.DAYS).toString())
                        .addArray("deliveryChannels", deliveryChannels)
                        .addObject("recipients", JsonObjectBuilder.jsonObject()
                                .addObject("organisationUnit", JsonObjectBuilder.jsonObject().addProperty("id",orgUnit)))
                        .addObject("enrollment", JsonObjectBuilder.jsonObject().addProperty("id",enrollment))
                        .build();

        JsonArray programMessageList = new JsonArray();
        programMessageList.add( programMessage );

        ApiResponse response = this.post(JsonObjectBuilder.jsonObject().addArray("programMessages", programMessageList).build());

        response.validate().statusCode(is(oneOf(201, 200)));
        return response.extractUid();
    }
}
