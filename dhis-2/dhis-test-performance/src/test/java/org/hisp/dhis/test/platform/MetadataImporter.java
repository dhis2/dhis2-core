/*
 * Copyright (c) 2004-2025, University of Oslo
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
