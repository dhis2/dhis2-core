/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.apps;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppResourceTest extends ApiTest {

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
        ApiResponse response
                = new ApiResponse(given().redirects().follow(false).get("/apps/test-minio"));

        // then redirect should be returned with trailing slash
        response.validate().header("location", equalTo("http://web:8080/api/apps/test-minio/"));
        response.validate().statusCode(302);
    }

    @Test
    @DisplayName("Bundled apps are served from /dhis-web-<app> paths with correct internal redirects")
    void bundledAppServedFromDhisWebPath() {
        List<String> apps = Arrays.asList("dhis-web-dashboard", "dhis-web-maintenance", "dhis-web-maps", "dhis-web-capture", "dhis-web-settings", "dhis-web-app-management");
        List<String> indexPaths = Arrays.asList("/", "/index.html");
        Map<String, String> redirects = Map.of(
                "", "/",
                "index.action", "/index.html"
        );

        apps.forEach(app -> {
            indexPaths.forEach(path -> {
                ApiResponse response = new ApiResponse(given().redirects().follow(false).get("/apps/dhis-web-commons/" + path));
                response.validate().statusCode(302);
                response.validate().header("location", equalTo("http://web:8080/dhis-web-commons/" + redirects.get(path)));
            });
            redirects.forEach((source, target) -> {
                ApiResponse response = new ApiResponse(given().redirects().follow(false).get("/" + app + source));
                response.validate().statusCode(302);
                response.validate().header("location", equalTo("http://web:8080/" + app + redirects.get(target)));
            });
        });
    }
}
