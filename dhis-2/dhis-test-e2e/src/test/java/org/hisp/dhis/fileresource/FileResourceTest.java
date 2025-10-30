package org.hisp.dhis.fileresource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.File;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FileResourceTest extends ApiTest {

  private RestApiActions documentApi;
  private RestApiActions fileResourceApi;
  private RestApiActions iconApi;
  private RestApiActions meApi;
  private RestApiActions usersApi;

  @BeforeAll
  void beforeAll() {
    documentApi = new RestApiActions("documents");
    fileResourceApi = new RestApiActions("fileResources");
    iconApi = new RestApiActions("icons");
    meApi = new RestApiActions("me");
    usersApi = new RestApiActions("users");
  }

  @Test
  @DisplayName("Creating and deleting a Document flips the FileResource assigned value")
  void documentFileResourceTest() {
    // given a FileResource exists
    File file = new File("src/test/resources/fileResources/dhis2.png");
    String frUid = postFileResource(file, "DOCUMENT");
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

    // and when the Document is deleted
    documentApi.delete("docUid000x1").validateStatus(200);

    // then the FileResource should now be unassigned
    fileResourceApi.get(frUid).validate().body("assigned", equalTo(false));
  }

  @Test
  @DisplayName("Creating and deleting an Icon flips the FileResource assigned value")
  void iconFileResourceTest() {
    // given a FileResource exists
    File file = new File("src/test/resources/fileResources/dhis2.png");
    String frUid = postFileResource(file, "ICON");
    assertTrue(frUid != null && !frUid.isEmpty());

    // and it is unassigned
    fileResourceApi.get(frUid).validate().body("assigned", equalTo(false));

    // when creating an Icon with a ref to the FileResource
    iconApi
        .post(
            """
            {
               "fileResourceId":"%s",
               "key": "dhis2-icon",
               "description": "icon test",
               "keywords": []
            }
            """
                .formatted(frUid))
        .validate()
        .statusCode(201);

    // then the FileResource should now be assigned
    fileResourceApi.get(frUid).validate().body("assigned", equalTo(true));

    // and when the Icon is deleted
    iconApi.delete("dhis2-icon").validateStatus(200);

    // then the FileResource should now be unassigned
    fileResourceApi.get(frUid).validate().body("assigned", equalTo(false));
  }

  @Test
  @DisplayName("Adding and then updating a user avatar flips the FileResource assigned value")
  void avatarUpdateFileResourceTest() {
    // given 2 FileResources exist
    File file1 = new File("src/test/resources/fileResources/dhis2.png");
    String frUid1 = postFileResource(file1, "USER_AVATAR");

    File file2 = new File("src/test/resources/fileResources/dhis3.png");
    String frUid2 = postFileResource(file2, "USER_AVATAR");

    assertTrue(frUid1 != null && !frUid1.isEmpty());
    assertTrue(frUid2 != null && !frUid2.isEmpty());

    // and they are unassigned
    fileResourceApi.get(frUid1).validate().body("assigned", equalTo(false));
    fileResourceApi.get(frUid2).validate().body("assigned", equalTo(false));

    // when adding the 1st avatar to a user
    meApi
        .put(
            """
        {
          "avatar": {
            "id": "%s"
          }
        }
        """
                .formatted(frUid1))
        .validate()
        .statusCode(200);

    // then the FileResource should now be assigned
    fileResourceApi.get(frUid1).validate().body("assigned", equalTo(true));

    // and when the avatar is updated
    meApi
        .put(
            """
        {
          "avatar": {
            "id": "%s"
          }
        }
        """
                .formatted(frUid2))
        .validate()
        .statusCode(200);

    // then the old FileResource should now be unassigned
    fileResourceApi.get(frUid1).validate().body("assigned", equalTo(false));

    // and the new FileResource should now be assigned
    fileResourceApi.get(frUid2).validate().body("assigned", equalTo(true));
  }

  @Test
  @DisplayName("Adding and then deleting a user avatar flips the FileResource assigned value")
  void avatarDeleteFileResourceTest() throws JsonProcessingException {
    // given a FileResources exists
    File file = new File("src/test/resources/fileResources/dhis2.png");
    String frUid = postFileResource(file, "USER_AVATAR");
    assertTrue(frUid != null && !frUid.isEmpty());

    // and it is unassigned
    fileResourceApi.get(frUid).validate().body("assigned", equalTo(false));

    // when adding the avatar to a user
    meApi
        .put(
            """
            {
              "avatar": {
                "id": "%s"
              }
            }
            """
                .formatted(frUid))
        .validate()
        .statusCode(200);

    // then the FileResource should now be assigned
    fileResourceApi.get(frUid).validate().body("assigned", equalTo(true));

    // and when the avatar is deleted
    // get full user 1st
    Response response =
        given()
            .when()
            .accept(ContentType.JSON)
            .get("/users/M5zQapPyTZI?fields=:owner")
            .then()
            .statusCode(200)
            .extract()
            .response();
    ObjectMapper mapper = new ObjectMapper();

    // Parse directly into JsonNode
    JsonNode jsonNode = mapper.readTree(response.asString());

    // Cast to ObjectNode if you want to manipulate it
    ObjectNode objectNode = (ObjectNode) jsonNode;

    // remove avatar
    objectNode.remove("avatar");
    // Convert ObjectNode to JSON string
    String payload = mapper.writeValueAsString(objectNode);

    // put user without avatar field
    usersApi.put("M5zQapPyTZI", payload).validate().statusCode(200);

    // then the FileResource should now be unassigned
    fileResourceApi.get(frUid).validate().body("assigned", equalTo(false));
  }

  private String postFileResource(File file, String domain) {
    return given()
        .multiPart("file", file, "image/png")
        .formParam("domain", domain)
        .contentType("multipart/form-data")
        .when()
        .post("/fileResources")
        .then()
        .statusCode(202)
        .extract()
        .path("response.fileResource.id");
  }
}
