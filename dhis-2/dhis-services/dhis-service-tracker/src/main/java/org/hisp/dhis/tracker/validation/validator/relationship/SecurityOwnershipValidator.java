/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.validation.validator.relationship;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.converter.RelationshipTrackerConverterService;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.validation.Reporter;
import org.hisp.dhis.tracker.validation.ValidationCode;
import org.hisp.dhis.tracker.validation.Validator;
import org.springframework.stereotype.Component;

@Component(
    "org.hisp.dhis.tracker.imports.validation.validator.relationship.SecurityOwnershipValidator")
@RequiredArgsConstructor
class SecurityOwnershipValidator implements Validator<Relationship> {
  private final TrackerAccessManager trackerAccessManager;
  private final RelationshipTrackerConverterService relationshipTrackerConverterService;

  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, Relationship relationship) {
    TrackerImportStrategy strategy = bundle.getStrategy(relationship);

    if (strategy.isDelete()
        && (!trackerAccessManager
            .canDelete(bundle.getUser(), bundle.getPreheat().getRelationship(relationship))
            .isEmpty())) {
      reporter.addError(
          relationship, ValidationCode.E4020, bundle.getUser(), relationship.getRelationship());
    }

    if (strategy.isCreate()
        && (!trackerAccessManager
            .canWrite(
                bundle.getUser(),
                relationshipTrackerConverterService.from(bundle.getPreheat(), relationship))
            .isEmpty())) {
      reporter.addError(
          relationship, ValidationCode.E4020, bundle.getUser(), relationship.getRelationship());
    }
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return true;
  }
}
