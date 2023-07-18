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
package org.hisp.dhis.dxf2.events.importer.mapper;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.common.IdScheme.CODE;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.hisp.dhis.event.EventStatus.fromInt;
import static org.hisp.dhis.util.DateUtils.parseDate;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;

/**
 * @author Luciano Fiandesio
 */
public class ProgramStageInstanceMapper extends AbstractMapper<Event, ProgramStageInstance> {
  private final ProgramStageInstanceNoteMapper noteMapper;

  public ProgramStageInstanceMapper(WorkContext ctx) {
    super(ctx);
    noteMapper = new ProgramStageInstanceNoteMapper(ctx);
  }

  @Override
  public ProgramStageInstance map(Event event) {
    ProgramStageInstance psi = workContext.getProgramStageInstanceMap().get(event.getUid());

    if (psi == null) {
      psi = mapForInsert(event);
    } else {
      mapForUpdate(event, psi);
    }

    return mapDataValueIdentifier(psi, super.workContext.getDataElementMap());
  }

  /**
   * Set data element identifier to uid. We run the psi through this method to make sure data value
   * identifiers are UID. Otherwise, we would store data values with whatever identifier is used for
   * import, for example CODE or ATTRIBUTE.
   *
   * @param psi the psi with the datavalues to normalize to uid identifiers
   * @param dataElementMap a map of persisted data elements.
   * @return a program stage instance with normalized data values.
   */
  private ProgramStageInstance mapDataValueIdentifier(
      ProgramStageInstance psi, Map<String, DataElement> dataElementMap) {
    for (EventDataValue dv : psi.getEventDataValues()) {
      DataElement de = dataElementMap.get(dv.getDataElement());
      if (de != null) {
        dv.setDataElement(de.getUid());
      }
    }
    return psi;
  }

  private ProgramStageInstance mapForUpdate(Event event, ProgramStageInstance psi) {
    // Program Instance
    workContext.getProgramInstance(event.getUid()).ifPresent(psi::setProgramInstance);

    // Program Stage
    getProgramStage(event).ifPresent(psi::setProgramStage);

    // Org Unit
    getOrganisationUnit(event).ifPresent(psi::setOrganisationUnit);

    // Status and completed date are set in the Update Preprocessor //

    // Attribute Option Combo
    psi.setAttributeOptionCombo(this.workContext.getCategoryOptionComboMap().get(event.getUid()));

    // Geometry
    psi.setGeometry(event.getGeometry());

    // Notes
    if (!event.getNotes().isEmpty()) {
      psi.setComments(convertNotes(event, this.workContext));
    }

    // Data Values
    psi.setEventDataValues(workContext.getEventDataValueMap().get(event.getUid()));

    if (event.getDueDate() != null) {
      psi.setDueDate(parseDate(event.getDueDate()));
    }

    setExecutionDate(event, psi);

    if (psi.getProgramStage() != null && psi.getProgramStage().isEnableUserAssignment()) {
      psi.setAssignedUser(this.workContext.getAssignedUserMap().get(event.getUid()));
    }

    // CREATED AT CLIENT + UPDATED AT CLIENT
    psi.setCreatedAtClient(parseDate(event.getCreatedAtClient()));
    psi.setLastUpdatedAtClient(parseDate(event.getLastUpdatedAtClient()));

    psi.setStoredBy(event.getStoredBy());
    psi.setCompletedBy(event.getCompletedBy());

    psi.setLastUpdatedByUserInfo(event.getLastUpdatedByUserInfo());

    return psi;
  }

  public ProgramStageInstance mapForInsert(Event event) {
    ImportOptions importOptions = workContext.getImportOptions();

    ProgramStageInstance psi = new ProgramStageInstance();

    if (importOptions.getIdSchemes().getProgramStageInstanceIdScheme().equals(CODE)) {
      psi.setCode(event.getEvent());
    } else if (importOptions.getIdSchemes().getProgramStageIdScheme().equals(UID)) {
      psi.setUid(event.getUid());
    }

    // Program Instance
    psi.setProgramInstance(this.workContext.getProgramInstanceMap().get(event.getUid()));

    // Program Stage
    psi.setProgramStage(
        this.workContext.getProgramStage(
            importOptions.getIdSchemes().getProgramStageIdScheme(), event.getProgramStage()));

    // Org Unit
    psi.setOrganisationUnit(this.workContext.getOrganisationUnitMap().get(event.getUid()));

    // Status
    psi.setStatus(fromInt(event.getStatus().getValue()));

    // Attribute Option Combo
    psi.setAttributeOptionCombo(this.workContext.getCategoryOptionComboMap().get(event.getUid()));

    // Geometry
    psi.setGeometry(event.getGeometry());

    // Notes
    psi.setComments(convertNotes(event, this.workContext));

    // Data Values
    psi.setEventDataValues(workContext.getEventDataValueMap().get(event.getUid()));

    Date dueDate = new Date();

    if (event.getDueDate() != null) {
      dueDate = parseDate(event.getDueDate());
    }

    psi.setDueDate(dueDate);
    setCompletedDate(event, psi);
    // Note that execution date can be null
    setExecutionDate(event, psi);

    if (psi.getProgramStage() != null && psi.getProgramStage().isEnableUserAssignment()) {
      psi.setAssignedUser(this.workContext.getAssignedUserMap().get(event.getUid()));
    }

    // CREATED AT CLIENT + UPDATED AT CLIENT
    psi.setCreatedAtClient(parseDate(event.getCreatedAtClient()));
    psi.setLastUpdatedAtClient(parseDate(event.getLastUpdatedAtClient()));

    psi.setStoredBy(event.getStoredBy());
    psi.setCompletedBy(event.getCompletedBy());

    psi.setCreatedByUserInfo(event.getCreatedByUserInfo());
    psi.setLastUpdatedByUserInfo(event.getLastUpdatedByUserInfo());

    return psi;
  }

  private List<TrackedEntityComment> convertNotes(Event event, WorkContext ctx) {
    if (isNotEmpty(event.getNotes())) {
      return event.getNotes().stream()
          .filter(note -> ctx.getNotesMap().containsKey(note.getNote()))
          .map(noteMapper::map)
          .collect(toList());
    }

    return emptyList();
  }

  private Optional<ProgramStage> getProgramStage(Event event) {
    return Optional.ofNullable(
        this.workContext.getProgramStage(
            this.workContext.getImportOptions().getIdSchemes().getProgramStageIdScheme(),
            event.getProgramStage()));
  }

  private Optional<OrganisationUnit> getOrganisationUnit(Event event) {
    return Optional.ofNullable(this.workContext.getOrganisationUnitMap().get(event.getUid()));
  }

  private void setExecutionDate(Event event, ProgramStageInstance psi) {
    if (event.getEventDate() != null) {
      psi.setExecutionDate(parseDate(event.getEventDate()));
    }
  }

  private void setCompletedDate(Event event, ProgramStageInstance psi) {
    // Completed Date // FIXME this logic should be moved to a preprocessor
    if (psi.isCompleted()) {
      Date completedDate = new Date();
      if (event.getCompletedDate() != null) {
        completedDate = parseDate(event.getCompletedDate());
      }
      psi.setCompletedDate(completedDate);
    }
  }
}
