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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.hisp.dhis.common.CodeGenerator.isValidUid;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramCategoryMapping;
import org.hisp.dhis.program.ProgramCategoryOptionMapping;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
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
  private final ProgramStageService programStageService;

  private final AclService aclService;

  @Override
  public void postCreate(Program object, ObjectBundle bundle) {
    syncSharingForEventProgram(object);

    updateProgramStage(object);
  }

  @Override
  public void postUpdate(Program object, ObjectBundle bundle) {
    syncSharingForEventProgram(object);
  }

  @Override
  public void validate(Program program, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    Program relatedProgram = program.getRelatedProgram();
    if (relatedProgram != null && Objects.equals(relatedProgram.getUid(), program.getUid())) {
      addReports.accept(new ErrorReport(Program.class, ErrorCode.E6022, "relatedProgram"));
    }
    if (relatedProgram != null && program.getProgramType().isEventProgram()) {
      addReports.accept(
          new ErrorReport(
              Program.class,
              ErrorCode.E4023,
              "relatedProgram",
              "programType",
              ProgramType.WITHOUT_REGISTRATION.name()));
    }
    validateAttributeSecurity(program, bundle, addReports);
    validateCategoryMappings(program, addReports);
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

              if (attribute == null || !aclService.canRead(bundle.getUserDetails(), attribute)) {
                addReports.accept(
                    new ErrorReport(
                        TrackedEntityAttribute.class,
                        ErrorCode.E3012,
                        identifier.getIdentifiersWithName(bundle.getUserDetails()),
                        identifier.getIdentifiersWithName(programAttr.getAttribute())));
              }
            });
  }

  /** Validates program category mappings. */
  private void validateCategoryMappings(Program program, Consumer<ErrorReport> addReports) {
    validateCategoryMappingUids(program, addReports);
    validateCategoryMappingNameUniqueness(program, addReports);
    validateCategoryOptionMappingUids(program, addReports);
  }

  /** Checks that mapping UIDs are valid and are unique within the program. */
  private void validateCategoryMappingUids(Program program, Consumer<ErrorReport> addReports) {
    Set<String> uniqueUids = new HashSet<>();

    for (ProgramCategoryMapping mapping : program.getCategoryMappings()) {
      String uid = mapping.getId();
      if (!isValidUid(uid))
        addReports.accept(new ErrorReport(Program.class, ErrorCode.E4075, program.getUid(), uid));

      if (uniqueUids.contains(uid)) {
        addReports.accept(new ErrorReport(Program.class, ErrorCode.E4076, program.getUid(), uid));
      }
      uniqueUids.add(uid);
    }
  }

  /** Checks that mapping names are unique within each category. */
  private void validateCategoryMappingNameUniqueness(
      Program program, Consumer<ErrorReport> addReports) {

    Set<String> uniqueMappingNames = new HashSet<>();
    for (ProgramCategoryMapping mapping : program.getCategoryMappings()) {
      String categoryAndMappingName = mapping.getCategoryId() + mapping.getMappingName();
      if (uniqueMappingNames.contains(categoryAndMappingName)) {
        addReports.accept(
            new ErrorReport(
                Program.class,
                ErrorCode.E4077,
                program.getUid(),
                mapping.getCategoryId(),
                mapping.getMappingName()));
      }
      uniqueMappingNames.add(categoryAndMappingName);
    }
  }

  /** Validates category option mapping UIDs. */
  private void validateCategoryOptionMappingUids(
      Program program, Consumer<ErrorReport> addReports) {
    for (ProgramCategoryMapping mapping : program.getCategoryMappings()) {
      Set<String> uniqueUids = new HashSet<>();
      for (ProgramCategoryOptionMapping optionMapping : mapping.getOptionMappings()) {
        String uid = optionMapping.getOptionId();
        if (!isValidUid(uid))
          addReports.accept(
              new ErrorReport(
                  Program.class, ErrorCode.E4080, program.getUid(), mapping.getId(), uid));

        if (uniqueUids.contains(uid)) {
          addReports.accept(
              new ErrorReport(
                  Program.class, ErrorCode.E4079, program.getUid(), mapping.getId(), uid));
        }
        uniqueUids.add(uid);
      }
    }
  }
}
