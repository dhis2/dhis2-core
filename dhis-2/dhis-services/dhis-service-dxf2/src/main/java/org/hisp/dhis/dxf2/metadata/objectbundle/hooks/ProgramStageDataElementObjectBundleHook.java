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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.function.Consumer;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.hisp.dhis.render.type.ValueTypeRenderingObject;
import org.hisp.dhis.system.util.ValidationUtils;
import org.springframework.stereotype.Component;

@Component
public class ProgramStageDataElementObjectBundleHook
    extends AbstractObjectBundleHook<ProgramStageDataElement> {
  /**
   * Validate that the RenderType (if any) conforms to the constraints of ValueType or OptionSet.
   */
  @Override
  public void validate(
      ProgramStageDataElement psda, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    DeviceRenderTypeMap<ValueTypeRenderingObject> map = psda.getRenderType();

    if (map == null) {
      return;
    }
    DataElement de = psda.getDataElement();
    for (ValueTypeRenderingObject renderingObject : map.values()) {
      if (renderingObject.getType() == null) {
        addReports.accept(
            new ErrorReport(ProgramStageDataElement.class, ErrorCode.E4011, "renderType.type"));
      }

      if (!ValidationUtils.validateRenderingType(
          ProgramStageDataElement.class,
          de.getValueType(),
          de.hasOptionSet(),
          renderingObject.getType())) {
        addReports.accept(
            new ErrorReport(
                ProgramStageDataElement.class,
                ErrorCode.E4017,
                renderingObject.getType(),
                de.getValueType()));
      }
    }
  }
}
