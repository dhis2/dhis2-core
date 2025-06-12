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
package org.hisp.dhis.apps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Tag("apptests")
class AppResourceTest extends ApiTest {

  private static final RestTemplate restTemplate =
      new RestTemplate(
          new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod)
                throws IOException {
              super.prepareConnection(connection, httpMethod);
              connection.setInstanceFollowRedirects(false);
            }
          });

  private static final String SERVER_BASE = "http://web:8080";
  private static final String META_TAG_DHIS2_BASE_URL =
      "<meta name=\"dhis2-base-url\" content=\"" + SERVER_BASE + "\">";

  @Test
  @DisplayName("Redirect location should have correct format")
  void redirectLocationCorrectFormatTest() {
    // given an app is installed
    File file = new File("src/test/resources/apps/test-app-v1.zip");
    given()
        .multiPart("file", file)
        .contentType("multipart/form-data")
        .when()
        .post("/apps")
        .then()
        .statusCode(201);

    // when called with missing trailing slash
    ApiResponse response =
        new ApiResponse(given().redirects().follow(false).get("/apps/test-minio"));

    // then redirect should be returned with trailing slash
    response.validate().header("location", equalTo("http://web:8080/api/apps/test-minio/"));
    response.validate().statusCode(302);
  }

  @ParameterizedTest
  @DisplayName("Bundled apps are served from /dhis-web-<app> paths with correct redirects")
  @ValueSource(
      strings = {"dashboard", "maintenance", "maps", "capture", "settings", "app-management"})
  void bundledAppOverridesServedFromApiApps(String app) {
    String prefix = "/dhis-web-";
    String newPrefix = "/api/apps/";

    //    // Redirect to global shell from index.html (default)
    //    {
    //      ResponseEntity<String> response = getAuthenticated(prefix + app +
    // "/index.html?answer=42");
    ////      assertEquals(HttpStatus.FOUND, response.getStatusCode());
    //      List<String> location = response.getHeaders().get("Location");
    //      assertNotNull(location);
    //      assertEquals(1, location.size());
    //      assertEquals(SERVER_BASE + "/apps/" + app + "?answer=42", location.get(0));
    //    }
    //
    //    // Redirect to global shell from / (default) with forwarded querystring
    //    {
    //      ResponseEntity<String> response = getAuthenticated(prefix + app + "/?answer=42");
    //      assertEquals(HttpStatus.FOUND, response.getStatusCode());
    //      List<String> location = response.getHeaders().get("Location");
    //      assertNotNull(location);
    //      assertEquals(1, location.size());
    //      assertEquals(SERVER_BASE + "/apps/" + app + "?answer=42", location.get(0));
    //    }

    // Serve index.html from index.html?redirect=false
    {
      ResponseEntity<String> response =
          getAuthenticated(prefix + app + "/index.html?redirect=false");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals("text/html;charset=UTF-8", response.getHeaders().getContentType().toString());
      assertTrue(response.getHeaders().getContentLength() > 0);
    }

    // Serve index.html from /?redirect=false
    {
      ResponseEntity<String> response = getAuthenticated(prefix + app + "/?redirect=false");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals("text/html;charset=UTF-8", response.getHeaders().getContentType().toString());
      assertTrue(response.getHeaders().getContentLength() > 0);
    }

    // Serve index.html from index.html?shell=false
    {
      ResponseEntity<String> response = getAuthenticated(prefix + app + "/index.html?shell=false");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals("text/html;charset=UTF-8", response.getHeaders().getContentType().toString());
      assertTrue(response.getHeaders().getContentLength() > 0);
    }

    // Serve index.html from /?shell=false
    {
      ResponseEntity<String> response = getAuthenticated(prefix + app + "/?shell=false");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals("text/html;charset=UTF-8", response.getHeaders().getContentType().toString());
      assertTrue(response.getHeaders().getContentLength() > 0);
    }

    //    // Append trailing slash and redirect
    //    {
    //      ResponseEntity<String> response = getAuthenticated(prefix + app);
    //      assertEquals(HttpStatus.FOUND, response.getStatusCode());
    //      List<String> location = response.getHeaders().get("Location");
    //      assertNotNull(location);
    //      assertEquals(1, location.size());
    //      assertEquals(SERVER_BASE + newPrefix + app + "/", location.get(0));
    //    }

    // Append trailing slash and redirect
    //    {
    //      ResponseEntity<String> response = getAuthenticated(prefix + app);
    //      assertEquals(HttpStatus.FOUND, response.getStatusCode());
    //      List<String> location = response.getHeaders().get("Location");
    //      assertNotNull(location);
    //      assertEquals(1, location.size());
    //      assertEquals(SERVER_BASE + newPrefix + app + "/", location.get(0));
    //    }

    // Append trailing slash and redirect, with forwarded query string
    {
      ResponseEntity<String> response = getAuthenticated(prefix + app + "?answer=42");
      assertEquals(HttpStatus.FOUND, response.getStatusCode());
      List<String> location = response.getHeaders().get("Location");
      assertNotNull(location);
      assertEquals(1, location.size());
      assertEquals(SERVER_BASE + newPrefix + app + "/?answer=42", location.get(0));
    }

    // Serve index.html from index.html (service-worker)
    {
      HttpHeaders headers = new HttpHeaders();
      headers.set("Referer", "/service-worker.js");
      ResponseEntity<String> response = getAuthenticated(prefix + app + "/index.html", headers);
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals("text/html;charset=UTF-8", response.getHeaders().getContentType().toString());
      assertTrue(response.getHeaders().getContentLength() > 0);
    }

    // Serve index.html from / (service-worker)
    {
      HttpHeaders headers = new HttpHeaders();
      headers.set("Referer", "/service-worker.js");
      ResponseEntity<String> response = getAuthenticated(prefix + app + "/", headers);
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals("text/html;charset=UTF-8", response.getHeaders().getContentType().toString());
      assertTrue(response.getHeaders().getContentLength() > 0);
    }

    // Redirect index.action
    {
      ResponseEntity<String> response = getAuthenticated(prefix + app + "/index.action");
      assertEquals(HttpStatus.FOUND, response.getStatusCode());
      List<String> location = response.getHeaders().get("Location");
      assertEquals(SERVER_BASE + newPrefix + app + "/index.html", location.get(0));
    }

    // manifest.webapp
    {
      ResponseEntity<String> response = getAuthenticated(prefix + app + "/manifest.webapp");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertTrue(response.getHeaders().getContentLength() > 0);
    }

    // another resource should return with 200
    {
      ResponseEntity<String> response = getAuthenticated(prefix + app + "/package.json");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertTrue(response.getHeaders().getContentLength() > 0);
    }

    // non-existent resource should give 404
    {
      ResponseEntity<String> response = getAuthenticated(prefix + app + "/nonexistent.txt");
      assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
  }

  @Test
  @DisplayName("Global shell is served from /apps paths with correct redirects")
  void globalShellServed() {
    String prefix = "/apps";

    // Serve global shell index.html from /apps
    {
      ResponseEntity<String> response = getAuthenticated(prefix);
      assertEquals(HttpStatus.MOVED_PERMANENTLY, response.getStatusCode());
      List<String> location = response.getHeaders().get("Location");
      assertNotNull(location);
      assertEquals(1, location.size());
      assertEquals(prefix + "/", location.get(0));
      // TODO: Confirm template replacement
    }

    // Serve global shell index.html from /apps/
    {
      ResponseEntity<String> response = getAuthenticated(prefix + "/");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertNotNull(response.getHeaders().getContentType());
      assertEquals("text/html;charset=UTF-8", response.getHeaders().getContentType().toString());
      assertTrue(response.getHeaders().getContentLength() > 0);
      // TODO: Confirm template replacement
    }

    // Serve global shell index.html from /apps/dashboard
    {
      ResponseEntity<String> response = getAuthenticated(prefix + "/dashboard");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertNotNull(response.getHeaders().getContentType());
      assertEquals("text/html;charset=UTF-8", response.getHeaders().getContentType().toString());
      assertTrue(response.getHeaders().getContentLength() > 0);
      // TODO: Confirm template replacement
    }

    // Serve global shell index.html from /apps/my-app
    {
      ResponseEntity<String> response = getAuthenticated(prefix + "/my-app");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertNotNull(response.getHeaders().getContentType());
      assertEquals("text/html;charset=UTF-8", response.getHeaders().getContentType().toString());
      assertTrue(response.getHeaders().getContentLength() > 0);
      // TODO: Confirm template replacement
    }

    // Redirect to bare path if trailing slash
    {
      ResponseEntity<String> response = getAuthenticated(prefix + "/dashboard/");
      assertEquals(HttpStatus.FOUND, response.getStatusCode());
      List<String> location = response.getHeaders().get("Location");
      assertNotNull(location);
      assertEquals(1, location.size());
      assertEquals(prefix + "/dashboard", location.get(0));
    }

    // Redirect to original app with ?shell=false
    // {
    //   ResponseEntity<String> response = getAuthenticated(prefix + "/dashboard?shell=false");
    //   assertEquals(HttpStatus.FOUND, response.getStatusCode());
    //   List<String> location = response.getHeaders().get("Location");
    //   assertNotNull(location);
    //   assertEquals(1, location.size());
    //   assertEquals(SERVER_BASE + "/dhis-web-dashboard/index.html?shell=false", location.get(0));
    // }

    // Global shell service-worker
    {
      ResponseEntity<String> response = getAuthenticated(prefix + "/service-worker.js");
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertTrue(response.getHeaders().getContentLength() > 0);
    }

    // Non-existant resource (at root)
    {
      ResponseEntity<String> response = getAuthenticated(prefix + "/nonexistent.txt");
      assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // Non-existant resource (at sub-dir)
    {
      ResponseEntity<String> response = getAuthenticated(prefix + "/static/nonexistent.txt");
      assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
  }

  // TODO: Installed apps
  // TODO: Inaccessible apps
  // TODO: Test when global shell disabled

  private ResponseEntity<String> get(String path, HttpHeaders headers) {
    try {
      return restTemplate.exchange(
          SERVER_BASE + path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    } catch (HttpClientErrorException e) {
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    }
  }

  private ResponseEntity<String> get(String path) {
    return get(path, new HttpHeaders());
  }

  private ResponseEntity<String> getAuthenticated(String path, HttpHeaders headers) {
    // TODO: Use cookies and test with multiple users
    String authHeader =
        Base64.getUrlEncoder().encodeToString("admin:district".getBytes(StandardCharsets.UTF_8));
    headers.set("Authorization", "Basic " + authHeader);
    return get(path, headers);
  }

  private ResponseEntity<String> getAuthenticated(String path) {
    return getAuthenticated(path, new HttpHeaders());
  }
}
