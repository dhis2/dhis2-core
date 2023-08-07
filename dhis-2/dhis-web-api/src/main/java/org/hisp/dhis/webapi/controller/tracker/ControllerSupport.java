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
package org.hisp.dhis.webapi.controller.tracker;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class ControllerSupport {
  private ControllerSupport() {
    throw new IllegalStateException("Utility class");
  }

  public static final String RESOURCE_PATH = "/tracker";

  /**
   * Ensures that {@code fieldsAdvocatedByWeb} advocated by {@link
   * org.hisp.dhis.webapi.controller.tracker.export} as orderable are in fact orderable by the
   * service. Web is responsible for mapping from the language users use (our API) to our internal
   * representation used in our services. This is to prevent web and service (store) from getting
   * out of sync.
   */
  public static void assertUserOrderableFieldsAreSupported(
      String entityName,
      Set<String> fieldsSupportedByService,
      Map<String, String> fieldsAdvocatedByWeb) {
    Set<String> unsupportedFields = new HashSet<>(fieldsAdvocatedByWeb.values());
    unsupportedFields.removeAll(fieldsSupportedByService);
    if (!unsupportedFields.isEmpty()) {
      Set<String> unsupportedFieldNames =
          fieldsAdvocatedByWeb.entrySet().stream()
              .filter(e -> unsupportedFields.contains(e.getValue()))
              .map(Entry::getKey)
              .collect(Collectors.toSet());
      throw new IllegalStateException(
          entityName
              + " controller supports ordering by "
              + String.join(", ", unsupportedFieldNames)
              + " while "
              + entityName
              + " service does not.");
    }
  }
}
