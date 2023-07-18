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
package org.hisp.dhis.dxf2.events.importer.insert.validation;

import static org.hisp.dhis.dxf2.importsummary.ImportSummary.error;
import static org.hisp.dhis.dxf2.importsummary.ImportSummary.success;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.dxf2.events.importer.Checker;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class ProgramOrgUnitCheck implements Checker {
  @Override
  public ImportSummary check(ImmutableEvent event, WorkContext ctx) {
    ProgramInstance programInstance = ctx.getProgramInstanceMap().get(event.getUid());

    final Map<Long, List<Long>> programWithOrgUnitsMap = ctx.getProgramWithOrgUnitsMap();
    final Map<String, OrganisationUnit> organisationUnitMap = ctx.getOrganisationUnitMap();

    if (programInstance != null) {
      final OrganisationUnit organisationUnit = organisationUnitMap.get(event.getUid());
      final Program program = programInstance.getProgram();

      if (organisationUnit != null && program != null) {
        List<Long> ouList = programWithOrgUnitsMap.get(program.getId());
        if (ouList == null || !ouList.contains(organisationUnit.getId())) {
          return error(
              "Program is not assigned to this Organisation Unit: " + event.getOrgUnit(),
              event.getEvent());
        }
      }
    }

    return success();
  }
}
