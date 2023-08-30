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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.Date;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
@AllArgsConstructor
public class ProgramObjectBundleHook extends AbstractObjectBundleHook<Program> {
  private final EnrollmentService enrollmentService;

  private final ProgramStageService programStageService;

  private final AclService aclService;

  @Override
  public void postCreate(Program object, ObjectBundle bundle) {
    syncSharingForEventProgram(object);

    addProgramInstance(object);

    updateProgramStage(object);
  }

  @Override
  public void postUpdate(Program object, ObjectBundle bundle) {
    syncSharingForEventProgram(object);
  }

  @Override
  public void validate(Program program, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    if (program.getId() != 0 && getProgramInstancesCount(program) > 1) {
      addReports.accept(new ErrorReport(Program.class, ErrorCode.E6000, program.getName()));
    }
    validateAttributeSecurity(program, bundle, addReports);
  }

  private void syncSharingForEventProgram(Program program) {
    if (ProgramType.WITHOUT_REGISTRATION != program.getProgramType()
        || program.getProgramStages().isEmpty()) {
      return;
    }

    ProgramStage programStage = program.getProgramStages().iterator().next();
    AccessStringHelper.copySharing(program, programStage);

    programStage.setCreatedBy(program.getCreatedBy());
    programStageService.updateProgramStage(programStage);
  }

  private void updateProgramStage(Program program) {
    if (program.getProgramStages().isEmpty()) {
      return;
    }

    program
        .getProgramStages()
        .forEach(
            ps -> {
              if (Objects.isNull(ps.getProgram())) {
                ps.setProgram(program);
              }
            });
  }

  private void addProgramInstance(Program program) {
    if (getProgramInstancesCount(program) == 0 && program.isWithoutRegistration()) {
      Enrollment pi = new Enrollment();
      pi.setEnrollmentDate(new Date());
      pi.setIncidentDate(new Date());
      pi.setProgram(program);
      pi.setStatus(ProgramStatus.ACTIVE);
      pi.setStoredBy("system-process");

      this.enrollmentService.addEnrollment(pi);
    }
  }

  private int getProgramInstancesCount(Program program) {
    return enrollmentService.getEnrollments(program, ProgramStatus.ACTIVE).size();
  }

  private void validateAttributeSecurity(
      Program program, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    if (program.getProgramAttributes().isEmpty()) {
      return;
    }

    PreheatIdentifier identifier = bundle.getPreheatIdentifier();

    program
        .getProgramAttributes()
        .forEach(
            programAttr -> {
              TrackedEntityAttribute attribute =
                  bundle.getPreheat().get(identifier, programAttr.getAttribute());

              if (attribute == null || !aclService.canRead(bundle.getUser(), attribute)) {
                addReports.accept(
                    new ErrorReport(
                        TrackedEntityAttribute.class,
                        ErrorCode.E3012,
                        identifier.getIdentifiersWithName(bundle.getUser()),
                        identifier.getIdentifiersWithName(programAttr.getAttribute())));
              }
            });
  }
}
