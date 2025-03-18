/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.test.e2e.actions.metadata;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.File;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.dto.MetadataApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;

/**
 * An important point to note about this class in relation to test data clean-up is that the
 * implemented methods in this class use the query param `importReportMode=FULL`. This param is
 * paramount if the metadata created needs to be deleted after a test completes. By passing this
 * param, `objectReports` are returned in the response, which contain UIDs which allow the test
 * framework to track what has been created, which then allows deletion of said objects after each
 * test. See {@link RestApiActions#saveCreatedObjects(ApiResponse)} for more info.
 *
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MetadataActions extends RestApiActions {
  public MetadataActions() {
    super("/metadata");
  }

  public MetadataApiResponse importMetadata(File file, String... queryParams) {
    QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
    queryParamsBuilder.addAll(queryParams);
    queryParamsBuilder.addAll("importReportMode=FULL");

    ApiResponse response = postFile(file, queryParamsBuilder);
    response.validate().statusCode(200);

    return new MetadataApiResponse(response);
  }

  public MetadataApiResponse importMetadataWithContentType(
      File file, String contentType, String... queryParams) {
    QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
    queryParamsBuilder.addAll(queryParams);
    queryParamsBuilder.addAll("importReportMode=FULL");

    ApiResponse response = postFileWithContentType(file, queryParamsBuilder, contentType);
    response.validate().statusCode(200);

    return new MetadataApiResponse(response);
  }

  public MetadataApiResponse importMetadata(JsonObject object, String... queryParams) {
    QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
    queryParamsBuilder.addAll(queryParams);
    queryParamsBuilder.addAll("atomicMode=OBJECT", "importReportMode=FULL");

    ApiResponse response = post(object, queryParamsBuilder);
    response.validate().statusCode(200);

    return new MetadataApiResponse(response);
  }

  public MetadataApiResponse importMetadata(String metadata, String... queryParams) {
    JsonObject json = new Gson().fromJson(metadata, JsonObject.class);
    ApiResponse response = importMetadata(json, queryParams);
    return new MetadataApiResponse(response);
  }

  public MetadataApiResponse importAndValidateMetadata(JsonObject object, String... queryParams) {
    ApiResponse response = importMetadata(object, queryParams);

    response
        .validate()
        .body("response.stats.ignored", not(equalTo(response.extract("response.stats.total"))));

    return new MetadataApiResponse(response);
  }

  public MetadataApiResponse importAndValidateMetadata(File file, String... queryParams) {
    ApiResponse response = importMetadata(file, queryParams);

    response
        .validate()
        .body("response.stats.ignored", not(equalTo(response.extract("response.stats.total"))));

    return new MetadataApiResponse(response);
  }
}
