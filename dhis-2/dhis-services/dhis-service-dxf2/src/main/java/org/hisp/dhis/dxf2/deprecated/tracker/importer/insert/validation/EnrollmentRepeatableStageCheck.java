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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.insert.validation;

import static org.hisp.dhis.dxf2.importsummary.ImportSummary.success;

import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.Checker;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.context.WorkContext;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class EnrollmentRepeatableStageCheck implements Checker {
  @Override
  public ImportSummary check(ImmutableEvent event, WorkContext ctx) {
    IdScheme scheme = ctx.getImportOptions().getIdSchemes().getProgramStageIdScheme();
    ProgramStage programStage = ctx.getProgramStage(scheme, event.getProgramStage());
    Enrollment enrollment = ctx.getProgramInstanceMap().get(event.getUid());
    Program program = ctx.getProgramsMap().get(event.getProgram());
    TrackedEntity tei = null;

    if (program.isRegistration()) {
      tei = ctx.getTrackedEntityInstanceMap().get(event.getUid()).getLeft();
    }

    /*
     * Enrollment should never be null. If it's null, the EnrollmentCheck
     * should report this anomaly.
     */
    // @formatter:off
    if (enrollment != null
        && tei != null
        && program.isRegistration()
        && !programStage.getRepeatable()
        && hasProgramStageInstance(
            ctx.getServiceDelegator().getJdbcTemplate(),
            enrollment.getId(),
            programStage.getId(),
            tei.getId())) {
      return new ImportSummary(
              ImportStatus.ERROR, "Program stage is not repeatable and an event already exists")
          .setReference(event.getEvent())
          .incrementIgnored();
    }
    // @formatter:on

    return success();
  }

  private boolean hasProgramStageInstance(
      JdbcTemplate jdbcTemplate,
      long programInstanceId,
      long programStageId,
      long trackedEntityInstanceId) {
    // @formatter:off
    final String sql =
        "select exists( "
            + "select * "
            + "from event psi "
            + "  join programinstance pi on psi.programinstanceid = pi.programinstanceid "
            + "where pi.programinstanceid = ? "
            + "  and psi.programstageid = ? "
            + "  and psi.deleted = false "
            + "  and pi.trackedentityinstanceid = ? "
            + "  and psi.status != 'SKIPPED'"
            + ")";
    // @formatter:on

    return jdbcTemplate.queryForObject(
        sql, Boolean.class, programInstanceId, programStageId, trackedEntityInstanceId);
  }
}
