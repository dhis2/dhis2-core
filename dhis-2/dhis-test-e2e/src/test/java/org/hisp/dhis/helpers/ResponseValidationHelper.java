/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.helpers;

import static org.hamcrest.CoreMatchers.isA;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.http.HttpStatus;
import org.hisp.dhis.dto.ApiResponse;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class ResponseValidationHelper {
  public static void validateObjectRemoval(ApiResponse response, String message) {
    assertEquals(200, response.statusCode(), message);
    validateObjectUpdateResponse(response);
  }

  public static void validateObjectCreation(ApiResponse response) {
    if (response.statusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED) {
      return;
    }

    response.validate().statusCode(201);
    validateObjectUpdateResponse(response);
  }

  public static void validateObjectUpdate(ApiResponse response, int statusCode) {
    response.validate().statusCode(statusCode);
    validateObjectUpdateResponse(response);
  }

  // TODO integrate with OPEN API 3 when it´s ready
  private static void validateObjectUpdateResponse(ApiResponse response) {
    response
        .validate()
        .body("response", isA(Object.class))
        .body("status", isA(String.class))
        .body("httpStatusCode", isA(Integer.class))
        .body("httpStatus", isA(String.class))
        .body("response.responseType", isA(String.class))
        .body("response.klass", isA(String.class))
        .body("response.uid", isA(String.class));
  }
}
