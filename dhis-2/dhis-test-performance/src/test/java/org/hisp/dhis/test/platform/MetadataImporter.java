package org.hisp.dhis.test.platform;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** General purpose class to import metadata. */
public class MetadataImporter {

  private static final HttpClient client = HttpClient.newHttpClient();

  /**
   * Import metadata from a file on the classpath, using the user credentials passed in. Supplying a
   * payload with UIDs will allow the same call to be made multiple times without getting errors
   * (1st call is seen as a create, subsequent calls seen as updates).
   *
   * @param fileName file name
   * @param username username
   * @param password password
   */
  public static void importJsonFile(String fileName, String username, String password) {
    try {
      String auth =
          Base64.getEncoder()
              .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

      try (InputStream is = MetadataImporter.class.getClassLoader().getResourceAsStream(fileName)) {
        String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        HttpRequest request =
            HttpRequest.newBuilder()
                .uri(URI.create(OrganisationUnitGroupsTest.BASE_URL + "/api/metadata"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + auth)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> metadataImportResponse =
            client.send(request, HttpResponse.BodyHandlers.ofString());
        if (metadataImportResponse.statusCode() == 200) {
          OrganisationUnitGroupsTest.logger.debug("Metadata import successful");
        } else {
          OrganisationUnitGroupsTest.logger.debug("Metadata import unsuccessful");
          throw new RuntimeException(
              "Problem importing metadata: " + metadataImportResponse.body());
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
