package org.hisp.dhis.document;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DocumentTest extends ApiTest {

  private RestApiActions documentApi;
  private RestApiActions fileResourceApi;

  @BeforeAll
  void beforeAll() {
    documentApi = new RestApiActions("documents");
    fileResourceApi = new RestApiActions("fileResources");
  }

  @AfterAll
  void cleanUp() {
    documentApi.delete("docUid000x1");
    // file resource can't be deleted through the API, only through the FileResourceCleanupJob
  }

  @Test
  @DisplayName("Creating a Document with a FileResource results in an assigned FileResource")
  void test() {
    // given a FileResource exists
    File file = new File("src/test/resources/fileResources/dhis2.png");
    String frUid =
        given()
            .multiPart("file", file)
            .formParam("domain", "DOCUMENT")
            .contentType("multipart/form-data")
            .when()
            .post("/fileResources")
            .then()
            .statusCode(202)
            .extract()
            .path("response.fileResource.id");

    assertTrue(frUid != null && !frUid.isEmpty());

    // and it is unassigned
    fileResourceApi.get(frUid).validate().body("assigned", equalTo(false));

    // when creating a Document with a ref to the FileResource
    documentApi
        .post(
            """
              {
                "id":"docUid000x1",
                "name": "doc1",
                "type": "UPLOAD_FILE",
                "attachment": false,
                "external": false,
                "url": "%s"
              }
              """
                .formatted(frUid))
        .validate()
        .statusCode(201);

    // then the FileResource should now be assigned
    fileResourceApi.get(frUid).validate().body("assigned", equalTo(true));
  }
}
