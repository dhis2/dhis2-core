/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.mapping.ExternalMapLayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link ExternalMapLayerObjectBundleHook}. */
class ExternalMapLayerObjectBundleHookTest {

  private final ExternalMapLayerObjectBundleHook hook = new ExternalMapLayerObjectBundleHook();

  @ParameterizedTest
  @ValueSource(
      strings = {
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCA",
        "data:image/png;base64,iVBORw0KGgo=",
        "data:image/png;base64,iVBORw0KGg==",
        "data:image/jpeg;base64,/9j/4AAQSkZJRg+/",
        "data:image/jpg;base64,/9j/4AAQSkZJRg",
        "data:image/webp;base64,UklGRiQAAABXRUJQ"
      })
  void testValidImage(String image) {
    assertTrue(validate(image).isEmpty());
  }

  @Test
  void testNullImageIsValid() {
    assertTrue(validate(null).isEmpty());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "invalid image",
        "  ",
        "",
        "data:image/png,AAAA",
        "data:image/png;AAAA",
        "data:image/png;base64,",
        "data:image/png;base64,not base64 !!!",
        "data:image/png;base64,AAAA===",
        "data:image/png;base64,AA=A",
        "data:image/gif;base64,R0lGODlh",
        "data:image/svg+xml;base64,PHN2Zz4=",
        "data:image/PNG;base64,iVBORw0KGgo=",
        "data:text/plain;base64,iVBORw0KGgo=",
        "data:image/png;base64,iVBORw0KGgo=\n",
        " data:image/png;base64,iVBORw0KGgo="
      })
  void testInvalidImage(String image) {
    List<ErrorReport> reports = validate(image);
    assertEquals(1, reports.size());
    assertEquals(ErrorCode.E1552, reports.get(0).getErrorCode());
    assertEquals("image", reports.get(0).getErrorProperty());
  }

  @Test
  void testLargeInvalidImageIsRejected() {
    String image = "data:image/png;base64," + "A".repeat(2_097_152) + "!";
    assertEquals(1, validate(image).size());
  }

  private List<ErrorReport> validate(String image) {
    ExternalMapLayer layer = new ExternalMapLayer();
    layer.setImage(image);
    List<ErrorReport> reports = new ArrayList<>();
    hook.validate(layer, null, reports::add);
    return reports;
  }
}
