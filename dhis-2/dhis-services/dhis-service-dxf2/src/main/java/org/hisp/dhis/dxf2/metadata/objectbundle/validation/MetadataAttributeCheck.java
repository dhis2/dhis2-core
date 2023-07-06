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
package org.hisp.dhis.dxf2.metadata.objectbundle.validation;

import static org.hisp.dhis.dxf2.metadata.objectbundle.validation.ValidationUtils.createObjectReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.adapter.BaseIdentifiableObject_;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.attribute.AttributeValidator;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.schema.Schema;
import org.springframework.stereotype.Component;

/**
 * This is one of the validators of the Metadata Import service.
 *
 * <p>It will validate Metadata {@link Attribute} of all importing objects by executing {@link
 * AttributeValidator}'s functions.
 *
 * @author viet
 */
@Component
@RequiredArgsConstructor
public class MetadataAttributeCheck implements ObjectValidationCheck {
  private final AttributeValidator attributeValidator;

  @Override
  public <T extends IdentifiableObject> void check(
      ObjectBundle bundle,
      Class<T> klass,
      List<T> persistedObjects,
      List<T> nonPersistedObjects,
      ImportStrategy importStrategy,
      ValidationContext ctx,
      Consumer<ObjectReport> addReports) {
    Schema schema = ctx.getSchemaService().getDynamicSchema(klass);
    List<T> objects = selectObjects(persistedObjects, nonPersistedObjects, importStrategy);

    if (objects.isEmpty()
        || !schema.hasPersistedProperty(BaseIdentifiableObject_.ATTRIBUTE_VALUES)) {
      return;
    }

    Map<String, Attribute> attributesMap =
        bundle.getPreheat().getAttributesByClass(klass) != null
            ? bundle.getPreheat().getAttributesByClass(klass)
            : Map.of();

    for (T object : objects) {
      if (CollectionUtils.isEmpty(object.getAttributeValues())) {
        continue;
      }

      List<ErrorReport> errorReports = new ArrayList<>();

      object
          .getAttributeValues()
          .forEach(
              av ->
                  getValueType(
                          av.getAttribute().getUid(),
                          attributesMap,
                          klass.getSimpleName(),
                          errorReports::add)
                      .ifPresent(
                          type ->
                              attributeValidator.validate(type, av.getValue(), errorReports::add)));

      if (!errorReports.isEmpty()) {
        addReports.accept(createObjectReport(errorReports, object, bundle));
        ctx.markForRemoval(object);
      }
    }
  }

  /**
   * Get {@link ValueType} of the given attributeId.
   *
   * <p>Return {@link ErrorCode#E6012} if the given {@link Attribute} is not assigned to current
   * klass.
   *
   * @param attributeId Id of the {@link Attribute} for checking.
   * @param valueTypeMap Map contains all attributes of current object.
   * @param klassName name of current class.
   * @param addError Consumer for {@link ErrorReport} if any.
   * @return {@link ValueType} if exists otherwise {@link Optional#empty()}
   */
  private Optional<ValueType> getValueType(
      String attributeId,
      Map<String, Attribute> valueTypeMap,
      String klassName,
      Consumer<ErrorReport> addError) {
    Attribute attribute = valueTypeMap.get(attributeId);
    if (attribute == null) {
      addError.accept(new ErrorReport(Attribute.class, ErrorCode.E6012, attributeId, klassName));
      return Optional.empty();
    }

    return Optional.of(attribute.getValueType());
  }
}
