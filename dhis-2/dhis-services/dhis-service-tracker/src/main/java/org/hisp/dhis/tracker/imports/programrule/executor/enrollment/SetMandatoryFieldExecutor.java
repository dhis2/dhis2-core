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
package org.hisp.dhis.tracker.imports.programrule.executor.enrollment;

import static org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue.error;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.imports.programrule.executor.RuleActionExecutor;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils;

/**
 * This executor checks if a field is not empty in the {@link TrackerBundle} @Author Enrico
 * Colasante
 */
@RequiredArgsConstructor
public class SetMandatoryFieldExecutor implements RuleActionExecutor<Enrollment> {
  private final UID ruleUid;

  private final UID attributeUid;

  @Override
  public Optional<ProgramRuleIssue> executeRuleAction(TrackerBundle bundle, Enrollment enrollment) {
    TrackerIdSchemeParams idSchemes = bundle.getPreheat().getIdSchemes();
    TrackedEntityAttribute ruleAttribute =
        bundle.getPreheat().getTrackedEntityAttribute(attributeUid.getValue());

    Set<MetadataIdentifier> programAttributes =
        enrollment.getAttributes().stream()
            .map(Attribute::getAttribute)
            .collect(Collectors.toSet());

    Set<MetadataIdentifier> tetAttributes =
        ValidationUtils.getTrackedEntityAttributes(bundle, enrollment.getTrackedEntity());

    boolean missesMandatoryAttribute =
        Stream.concat(programAttributes.stream(), tetAttributes.stream())
            .noneMatch(a -> a.isEqualTo(ruleAttribute));

    if (missesMandatoryAttribute) {
      return Optional.of(
          error(
              ruleUid,
              ValidationCode.E1306,
              idSchemes.toMetadataIdentifier(ruleAttribute).getIdentifierOrAttributeValue()));
    }

    Optional<Attribute> programAttribute =
        enrollment.getAttributes().stream()
            .filter(attribute -> attribute.getAttribute().isEqualTo(ruleAttribute))
            .findAny();

    if (programAttribute.isPresent() && StringUtils.isEmpty(programAttribute.get().getValue())) {
      return Optional.of(
          error(
              ruleUid,
              ValidationCode.E1317,
              idSchemes.toMetadataIdentifier(ruleAttribute).getIdentifierOrAttributeValue()));
    }
    return Optional.empty();
  }
}
