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
package org.hisp.dhis.webapi.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface ContextService {
  /**
   * Get servlet path.
   *
   * @return the full href servlet path.
   */
  String getServletPath();

  /**
   * Get context path to API.
   *
   * @return the context path to API.
   */
  String getContextPath();

  /**
   * Get path to API.
   *
   * @return the API path.
   */
  String getApiPath();

  /**
   * Get active HttpServletRequest.
   *
   * @return the active HttpServletRequest.
   */
  HttpServletRequest getRequest();

  /**
   * Returns a list of values from a parameter, if the parameter doesn't exist, it will return a
   * empty list.
   *
   * @param name the parameter to get.
   * @return a list of parameter values, or empty if not found.
   */
  List<String> getParameterValues(String name);

  /**
   * Get all parameters as a mapping of key to values, supports more than one value per key (so
   * values is a collection).
   */
  Map<String, List<String>> getParameterValuesMap();

  /**
   * Get a list of fields from request.
   *
   * @return a list of fields.
   */
  List<String> getFieldsFromRequestOrAll();

  /**
   * Get a list of fields from request, or the given default fields.
   *
   * @param fields the default fields.
   * @return a list of fields.
   */
  List<String> getFieldsFromRequestOrElse(String fields);
}
