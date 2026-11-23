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

import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.mapping.ExternalMapLayer;
import org.springframework.stereotype.Component;

/**
 * Validates {@link ExternalMapLayer} objects on all metadata import paths (POST, PUT, JSON PATCH
 * and /api/metadata).
 *
 * <p>The image, when present, must be a base64 encoded data URL of a PNG, JPEG or WEBP image.
 */
@Component
public class ExternalMapLayerObjectBundleHook extends AbstractObjectBundleHook<ExternalMapLayer> {

  /**
   * Base64 data URL of a supported image type. The base64 alphabet uses a possessive quantifier to
   * guarantee linear matching time for large (up to 2 MB) values.
   */
  static final Pattern VALID_IMAGE =
      Pattern.compile("^data:image/(png|jpe?g|webp);base64,[A-Za-z0-9+/]++={0,2}$");

  @Override
  public void validate(
      ExternalMapLayer externalMapLayer, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    String image = externalMapLayer.getImage();

    if (image != null && !isValidImage(image)) {
      addReports.accept(
          new ErrorReport(ExternalMapLayer.class, ErrorCode.E1552)
              .setErrorProperty("image")
              .setMainId(externalMapLayer.getUid()));
    }
  }

  static boolean isValidImage(String image) {
    return image != null && VALID_IMAGE.matcher(image).matches();
  }
}
