package org.hisp.dhis.apps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppResourceTest extends ApiTest {

  private final RestApiActions appActions = new RestApiActions("/api/apps");

  @Test
  @DisplayName("Redirect location should have correct format")
  void redirectLocationCorrectFormatTest() {
    // given an app is installed
    File file = new File("src/test/resources/apps/test-app-v1.zip");
    appActions.postMultiPartFile(file).validateStatus(204);

    // when
    // called with missing trailing slash
    ApiResponse response =
        new ApiResponse(given().redirects().follow(false).get("/apps/test-minio"));

    // then redirect should be returned with trailing slash
    response.validate().header("location", equalTo("http://localhost:8080/api/apps/test-minio/"));
    response.validate().statusCode(302);
  }
}
