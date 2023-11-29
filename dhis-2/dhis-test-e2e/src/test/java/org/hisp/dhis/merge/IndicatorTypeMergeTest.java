package org.hisp.dhis.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndicatorTypeMergeTest extends ApiTest {

  private RestApiActions indicatorTypeApiActions;
  private LoginActions loginActions;

  @BeforeEach
  public void before() {
    loginActions = new LoginActions();
    indicatorTypeApiActions = new RestApiActions("indicatorTypes");

    loginActions.loginAsSuperUser();
  }

  @Test
  void testThis() {
    String merge = indicatorTypeApiActions.post("merge", getMergeBody()).getAsString();
    assertEquals(
        "{\"httpStatus\":\"OK\",\"httpStatusCode\":200,\"status\":\"OK\",\"response\":{\"mergeReport\":{\"mergeErrors\":[],\"mergeType\":\"INDICATOR_TYPE\",\"sourcesDeleted\":[]},\"message\":\"INDICATOR_TYPE merge complete\"}}",
        merge);
  }

  private JsonObject getMergeBody() {
    JsonObject json = new JsonObject();
    JsonArray array = new JsonArray();
    array.add("indType0001");
    array.add("indType0002");
    json.add("sources", array);
    json.addProperty("target", "indType0003");
    return json;
  }
}
