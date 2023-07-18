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
package org.hisp.dhis.webapi.controller.event.webrequest;

import static org.hisp.dhis.webapi.controller.event.webrequest.tracker.FieldTranslatorSupport.translate;

import java.util.Date;
import java.util.Optional;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.program.ProgramStatus;

/**
 * Class to hold EnrollmentController request parameters into a handy place
 *
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
@Data
@NoArgsConstructor
public class EnrollmentCriteria extends PagingAndSortingCriteriaAdapter {
  private String ou;

  private OrganisationUnitSelectionMode ouMode;

  private String program;

  private ProgramStatus programStatus;

  private Boolean followUp;

  private Date lastUpdated;

  private String lastUpdatedDuration;

  private Date programStartDate;

  private Date programEndDate;

  private String trackedEntityType;

  private String trackedEntityInstance;

  private String enrollment;

  private boolean includeDeleted;

  private Boolean paging;

  @Override
  public Optional<String> translateField(String dtoFieldName, boolean isLegacy) {
    return translate(dtoFieldName, LegacyDtoToEntityFieldTranslator.values());
  }

  /** Dto to database field translator for old tracker Enrollment export controller */
  @RequiredArgsConstructor
  private enum LegacyDtoToEntityFieldTranslator implements EntityNameSupplier {
    /**
     * this enum names must be the same as org.hisp.dhis.dxf2.events.enrollment.Enrollment fields,
     * just with different case
     *
     * <p>example: org.hisp.dhis.dxf2.events.enrollment.Enrollment.lastUpdated --> LAST_UPDATED
     */
    ENROLLMENT("uid"),
    TRACKED_ENTITY("pi.entityInstance.uid"),
    TRACKED_ENTITY_INSTANCE("pi.entityInstance.uid");

    @Getter private final String entityName;
  }
}
