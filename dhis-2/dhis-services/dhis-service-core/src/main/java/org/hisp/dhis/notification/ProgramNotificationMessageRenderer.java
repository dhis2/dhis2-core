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
package org.hisp.dhis.notification;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.notification.ProgramTemplateVariable;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.springframework.stereotype.Component;

/**
 * @author Halvdan Hoem Grelland
 */
@Component
public class ProgramNotificationMessageRenderer
    extends BaseNotificationMessageRenderer<Enrollment> {
  public static final ImmutableMap<TemplateVariable, Function<Enrollment, String>>
      VARIABLE_RESOLVERS =
          new ImmutableMap.Builder<TemplateVariable, Function<Enrollment, String>>()
              .put(ProgramTemplateVariable.PROGRAM_NAME, e -> e.getProgram().getDisplayName())
              .put(
                  ProgramTemplateVariable.ORG_UNIT_NAME,
                  e -> e.getOrganisationUnit().getDisplayName())
              .put(ProgramTemplateVariable.CURRENT_DATE, e -> formatDate(new Date()))
              .put(ProgramTemplateVariable.ENROLLMENT_DATE, e -> formatDate(e.getEnrollmentDate()))
              .put(ProgramTemplateVariable.INCIDENT_DATE, e -> formatDate(e.getIncidentDate()))
              .put(
                  ProgramTemplateVariable.DAYS_SINCE_ENROLLMENT_DATE,
                  e -> daysSince(e.getEnrollmentDate()))
              .put(
                  ProgramTemplateVariable.ENROLLMENT_ORG_UNIT_ID,
                  e -> e.getOrganisationUnit().getUid())
              .put(
                  ProgramTemplateVariable.ENROLLMENT_ORG_UNIT_NAME,
                  e -> e.getOrganisationUnit().getName())
              .put(
                  ProgramTemplateVariable.ENROLLMENT_ORG_UNIT_CODE,
                  e -> e.getOrganisationUnit().getCode())
              .put(ProgramTemplateVariable.PROGRAM_ID, e -> e.getProgram().getUid())
              .put(ProgramTemplateVariable.ENROLLMENT_ID, Enrollment::getUid)
              .put(ProgramTemplateVariable.TRACKED_ENTITY_ID, e -> e.getTrackedEntity().getUid())
              .build();

  private static final Set<ExpressionType> SUPPORTED_EXPRESSION_TYPES =
      ImmutableSet.of(ExpressionType.TRACKED_ENTITY_ATTRIBUTE, ExpressionType.VARIABLE);

  // -------------------------------------------------------------------------
  // Overrides
  // -------------------------------------------------------------------------

  @Override
  protected ImmutableMap<TemplateVariable, Function<Enrollment, String>> getVariableResolvers() {
    return VARIABLE_RESOLVERS;
  }

  @Override
  protected Map<String, String> resolveTrackedEntityAttributeValues(
      Set<String> attributeKeys, Enrollment entity) {
    if (attributeKeys.isEmpty()) {
      return Maps.newHashMap();
    }

    return entity.getTrackedEntity().getTrackedEntityAttributeValues().stream()
        .filter(av -> attributeKeys.contains(av.getAttribute().getUid()))
        .collect(
            Collectors.toMap(
                av -> av.getAttribute().getUid(), ProgramNotificationMessageRenderer::filterValue));
  }

  @Override
  protected TemplateVariable fromVariableName(String name) {
    return ProgramTemplateVariable.fromVariableName(name);
  }

  @Override
  protected Set<ExpressionType> getSupportedExpressionTypes() {
    return SUPPORTED_EXPRESSION_TYPES;
  }

  @Override
  protected Map<String, String> resolveDataElementValues(
      Set<String> elementKeys, Enrollment entity) {
    // DataElements are not supported for program notifications
    return Collections.emptyMap();
  }

  // -------------------------------------------------------------------------
  // Internal methods
  // -------------------------------------------------------------------------

  private static String filterValue(TrackedEntityAttributeValue av) {
    String value = av.getPlainValue();

    if (value == null) {
      return CONFIDENTIAL_VALUE_REPLACEMENT;
    }

    // If the AV has an OptionSet -> substitute value with the name of the
    // Option
    if (av.getAttribute().hasOptionSet()) {
      value = av.getAttribute().getOptionSet().getOptionByCode(value).getName();
    }

    return value != null ? value : MISSING_VALUE_REPLACEMENT;
  }
}
