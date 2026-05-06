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
package org.hisp.dhis.tracker.imports.validation.validator.event;

import static org.hisp.dhis.tracker.imports.bundle.TrackerObjectsMapper.mapSingleEvent;

import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.SingleEvent;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.springframework.stereotype.Component;

@Component("org.hisp.dhis.tracker.imports.validation.validator.event.SecuritySingleEventValidator")
@RequiredArgsConstructor
class SecuritySingleEventValidator
    implements Validator<org.hisp.dhis.tracker.imports.domain.Event> {

  @Nonnull private final TrackerAccessManager trackerAccessManager;

  @Override
  public void validate(
      Reporter reporter, TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.Event event) {
    if (!(event instanceof SingleEvent singleEvent)) {
      return;
    }

    TrackerImportStrategy strategy = bundle.getStrategy(event);

    if (strategy.isCreate()) {
      handleCreate(reporter, bundle, singleEvent);
    } else {
      org.hisp.dhis.tracker.model.SingleEvent databaseSingleEvent =
          bundle.getPreheat().getSingleEvent(singleEvent.getUID());

      if (strategy.isUpdate()) {
        handleUpdate(reporter, bundle, databaseSingleEvent, singleEvent);
      } else if (strategy.isDelete()) {
        handleDelete(reporter, bundle, databaseSingleEvent, singleEvent);
      }
    }
  }

  private void handleCreate(Reporter reporter, TrackerBundle bundle, SingleEvent singleEvent) {
    org.hisp.dhis.tracker.model.SingleEvent mappedEvent =
        mapSingleEvent(bundle.getPreheat(), singleEvent, bundle.getUser());

    trackerAccessManager
        .canCreate(bundle.getUser(), mappedEvent)
        .forEach(em -> reporter.addError(singleEvent, em.validationCode(), em.args().toArray()));
  }

  private void handleUpdate(
      Reporter reporter,
      TrackerBundle bundle,
      org.hisp.dhis.tracker.model.SingleEvent databaseSingleEvent,
      SingleEvent singleEvent) {
    OrganisationUnit payloadOrgUnit =
        bundle.getPreheat().getOrganisationUnit(singleEvent.getOrgUnit());
    OrganisationUnit orgUnit =
        payloadOrgUnit != null ? payloadOrgUnit : databaseSingleEvent.getOrganisationUnit();

    CategoryOptionCombo payloadAoc =
        bundle.getPreheat().getCategoryOptionCombo(singleEvent.getAttributeOptionCombo());
    CategoryOptionCombo aoc =
        payloadAoc != null ? payloadAoc : databaseSingleEvent.getAttributeOptionCombo();

    trackerAccessManager
        .canUpdate(bundle.getUser(), databaseSingleEvent, orgUnit, aoc)
        .forEach(em -> reporter.addError(singleEvent, em.validationCode(), em.args().toArray()));
  }

  private void handleDelete(
      Reporter reporter,
      TrackerBundle bundle,
      org.hisp.dhis.tracker.model.SingleEvent databaseSingleEvent,
      SingleEvent singleEvent) {
    trackerAccessManager
        .canDelete(bundle.getUser(), databaseSingleEvent)
        .forEach(em -> reporter.addError(singleEvent, em.validationCode(), em.args().toArray()));
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return true;
  }
}
