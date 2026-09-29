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
package org.hisp.dhis.tracker.imports.validation.validator;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1077;

import java.util.Objects;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preheat.UniqueAttributeValue;
import org.hisp.dhis.tracker.imports.util.Constant;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.model.TrackedEntity;

/**
 * @author Luciano Fiandesio
 */
public abstract class AttributeValidator {

  protected AttributeValidator() {}

  public void validateAttributeValue(Reporter reporter, TrackerDto trackerDto, String value) {
    // Validate value (string) don't exceed the max length
    reporter.addErrorIf(
        () -> value.length() > Constant.MAX_ATTR_VALUE_LENGTH,
        trackerDto,
        E1077,
        value,
        Constant.MAX_ATTR_VALUE_LENGTH);
  }

  protected void validateAttributeUniqueness(
      Reporter reporter,
      TrackerPreheat preheat,
      TrackerDto dto,
      String value,
      TrackedEntityAttribute trackedEntityAttribute,
      TrackedEntity trackedEntity,
      OrganisationUnit organisationUnit) {
    if (Boolean.FALSE.equals(trackedEntityAttribute.isUnique())) return;

    for (UniqueAttributeValue uniqueAttributeValue : preheat.getUniqueAttributeValues(value)) {
      boolean isTheSameTea = uniqueAttributeValue.attribute().isEqualTo(trackedEntityAttribute);
      boolean hasTheSameValue = value.equalsIgnoreCase(uniqueAttributeValue.value());
      boolean isNotSameTei =
          trackedEntity == null
              || !Objects.equals(trackedEntity.getUID(), uniqueAttributeValue.te());
      // the org unit of a value is only known when its attribute is unique within an org unit
      boolean isTeaUniqueInOrgUnitScope =
          !trackedEntityAttribute.getOrgUnitScopeNullSafe()
              || (uniqueAttributeValue.orgUnit() != null
                  && uniqueAttributeValue.orgUnit().isEqualTo(organisationUnit));

      if (isTheSameTea && hasTheSameValue && isNotSameTei && isTeaUniqueInOrgUnitScope) {
        reporter.addError(dto, ValidationCode.E1064, value, trackedEntityAttribute);
        return;
      }
    }
  }
}
