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

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.hibernate.Session;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramStageSectionService;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.stereotype.Component;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@Component
@AllArgsConstructor
public class ProgramStageObjectBundleHook extends AbstractObjectBundleHook<ProgramStage> {
  private final AclService aclService;

  private final ProgramStageSectionService programStageSectionService;

  @Override
  public void validate(
      ProgramStage programStage, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    if (programStage.getNextScheduleDate() != null) {
      DataElement nextScheduleDate =
          bundle
              .getPreheat()
              .get(
                  bundle.getPreheatIdentifier(),
                  DataElement.class,
                  programStage.getNextScheduleDate().getUid());

      if (!programStage.getDataElements().contains(programStage.getNextScheduleDate())
          || nextScheduleDate == null
          || !nextScheduleDate.getValueType().equals(ValueType.DATE)) {
        addReports.accept(
            new ErrorReport(
                ProgramStage.class,
                ErrorCode.E6001,
                programStage.getUid(),
                programStage.getNextScheduleDate().getUid()));
      }
    }

    validateProgramStageDataElementsAcl(programStage, bundle, addReports);

    if (programStage.getProgram() == null
        && !checkProgramReference(programStage.getUid(), bundle)) {
      addReports.accept(
          new ErrorReport(ProgramStage.class, ErrorCode.E4053, programStage.getUid()));
    }
  }

  @Override
  public void postCreate(ProgramStage programStage, ObjectBundle bundle) {
    Session session = sessionFactory.getCurrentSession();

    updateProgramStageSections(session, programStage);
  }

  @Override
  public void preUpdate(ProgramStage object, ProgramStage persistedObject, ObjectBundle bundle) {
    if (object == null || !object.getClass().isAssignableFrom(ProgramStage.class)) return;

    deleteRemovedSection(persistedObject, object);
  }

  private void deleteRemovedSection(
      ProgramStage persistedProgramStage, ProgramStage importProgramStage) {
    List<String> importIds =
        importProgramStage.getProgramStageSections().stream()
            .map(BaseIdentifiableObject::getUid)
            .collect(Collectors.toList());

    List<ProgramStageSection> programStageSectionsToDelete =
        persistedProgramStage.getProgramStageSections().stream()
            .filter(section -> !importIds.contains(section.getUid()))
            .peek(programStageSectionService::deleteProgramStageSection)
            .collect(Collectors.toList());

    persistedProgramStage.getProgramStageSections().removeAll(programStageSectionsToDelete);
  }

  private void updateProgramStageSections(Session session, ProgramStage programStage) {
    if (programStage.getProgramStageSections().isEmpty()) {
      return;
    }

    programStage
        .getProgramStageSections()
        .forEach(
            pss -> {
              if (pss.getProgramStage() == null) {
                pss.setProgramStage(programStage);
              }
            });

    session.update(programStage);
  }

  private void validateProgramStageDataElementsAcl(
      ProgramStage programStage, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    if (programStage.getDataElements().isEmpty()) {
      return;
    }

    PreheatIdentifier identifier = bundle.getPreheatIdentifier();

    programStage
        .getDataElements()
        .forEach(
            de -> {
              DataElement dataElement = bundle.getPreheat().get(identifier, de);

              if (dataElement == null || !aclService.canRead(bundle.getUser(), de)) {
                addReports.accept(
                    new ErrorReport(
                        DataElement.class,
                        ErrorCode.E3012,
                        identifier.getIdentifiersWithName(bundle.getUser()),
                        identifier.getIdentifiersWithName(de)));
              }
            });
  }

  /** Check if current ProgramStage has reference from a Program in same payload. */
  private boolean checkProgramReference(String programStageId, ObjectBundle objectBundle) {
    for (Program program : objectBundle.getObjects(Program.class)) {
      if (program.getProgramStages().stream().anyMatch(ps -> ps.getUid().equals(programStageId))) {
        return true;
      }
    }

    return false;
  }
}
