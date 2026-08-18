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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.ProgramStageDataElements;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * Projects the data elements of the preheated program stages instead of loading them as Hibernate
 * entities.
 *
 * <p>A program stage can have thousands of data elements while an event references a handful.
 * Mapping the {@code programStageDataElements} association initialized every one of them, which
 * dominated preheat on wide programs. Only three things are asked of that association during import
 * and all of them need an identifier and nothing else:
 *
 * <ul>
 *   <li>the compulsory data elements, to report one missing from the event (E1303) or being deleted
 *       by it (E1076)
 *   <li>whether a payload data element belongs to the stage (E1305)
 *   <li>whether a program rule effect targets a data element of the stage
 * </ul>
 *
 * <p>The last two are membership tests whose probes are known up front, so a data element that
 * neither the payload nor the rules mention cannot change their outcome and does not need to be
 * fetched. What is loaded is therefore bounded by the compulsory count plus the payload and rule
 * width, rather than by the width of the program.
 *
 * <p>Data elements referenced by the payload or by program rules are loaded as full entities
 * elsewhere, as validation needs their value type and option set. This supplier does not replace
 * that, it only avoids loading the rest of the stage.
 */
@Component
public class ProgramStageDataElementsSupplier extends JdbcAbstractPreheatSupplier {

  /**
   * Unioned rather than {@code or}ed, which measured faster on a program with thousands of data
   * elements: an {@code or} spanning both tables leaves Postgres pairing the stage's rows with the
   * payload's data elements and discarding the non-matches with a join filter, then sorting to
   * deduplicate.
   *
   * <p>{@code = any(array)} rather than {@code in (...)} so that an empty data element array stays
   * valid SQL matching nothing, instead of the syntax error {@code in ()} would be.
   */
  private static final String SQL =
      """
      select psde.programstageid, psde.compulsory, de.uid, de.code, de.name, de.attributevalues
      from programstagedataelement psde
      join dataelement de on de.dataelementid = psde.dataelementid
      where psde.programstageid = any(:programStageIds)
        and psde.compulsory
      union
      select psde.programstageid, psde.compulsory, de.uid, de.code, de.name, de.attributevalues
      from programstagedataelement psde
      join dataelement de on de.dataelementid = psde.dataelementid
      where psde.programstageid = any(:programStageIds)
        and de.uid = any(:dataElementUids)
      """;

  protected ProgramStageDataElementsSupplier(JdbcTemplate jdbcTemplate) {
    super(jdbcTemplate);
  }

  @Override
  public void preheatAdd(TrackerObjects trackerObjects, TrackerPreheat preheat) {
    // Stages named by the payload, plus the stages of every preheated program. An event in an
    // event program does not carry a program stage, EventProgramPreProcessor derives it from the
    // program, but that runs after preheat. Projecting the program's stages as well means the
    // derived stage is covered. Without this E1305 rejects every data value of such an event.
    Map<Long, ProgramStage> programStagesById =
        Stream.concat(
                preheat.getAll(ProgramStage.class).stream(),
                preheat.getAll(Program.class).stream()
                    .map(Program::getProgramStages)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream))
            .collect(Collectors.toMap(IdentifiableObject::getId, ps -> ps, (a, b) -> a));

    if (programStagesById.isEmpty()) {
      return;
    }

    // Data elements of the payload and of the program rules, both preheated by
    // TrackerIdentifierCollector, so reading their uids costs nothing. Program rules only ever
    // refer to data elements by uid.
    //
    // Note the rule ones are only preheated under idScheme=UID. The collector adds them as uids but
    // into the same identifier set as the payload's, which is then queried in the payload's
    // idScheme, so under CODE|NAME|ATTRIBUTE they do not load and drop out of this probe set. That
    // is pre-existing, see AbstractSchemaStrategy#generateRestrictionFromIdentifiers.
    Set<String> dataElementUids =
        preheat.getAll(DataElement.class).stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet());

    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("programStageIds", programStagesById.keySet().toArray(new Long[0]));
    parameters.addValue("dataElementUids", dataElementUids.toArray(new String[0]));

    TrackerIdSchemeParam idScheme = preheat.getIdSchemes().getDataElementIdScheme();

    Map<Long, Set<MetadataIdentifier>> compulsory = new HashMap<>();
    Map<Long, Set<MetadataIdentifier>> members = new HashMap<>();
    Map<Long, Set<UID>> memberUids = new HashMap<>();

    jdbcTemplate.query(
        SQL,
        parameters,
        rs -> {
          long programStageId = rs.getLong("programstageid");
          MetadataIdentifier identifier = toMetadataIdentifier(idScheme, rs);

          members.computeIfAbsent(programStageId, k -> new HashSet<>()).add(identifier);
          memberUids
              .computeIfAbsent(programStageId, k -> new HashSet<>())
              .add(UID.of(rs.getString("uid")));
          if (rs.getBoolean("compulsory")) {
            compulsory.computeIfAbsent(programStageId, k -> new HashSet<>()).add(identifier);
          }
        });

    programStagesById.forEach(
        (programStageId, programStage) ->
            preheat.putProgramStageDataElements(
                UID.of(programStage),
                new ProgramStageDataElements(
                    compulsory.getOrDefault(programStageId, Set.of()),
                    members.getOrDefault(programStageId, Set.of()),
                    memberUids.getOrDefault(programStageId, Set.of()))));
  }

  /**
   * Builds the identifier in the requested idScheme. Every column an idScheme can resolve to is
   * projected, so this has to pick the right one where {@link
   * TrackerIdSchemeParam#toMetadataIdentifier(IdentifiableObject)} would read it off the entity.
   */
  private static MetadataIdentifier toMetadataIdentifier(
      TrackerIdSchemeParam idScheme, ResultSet rs) throws SQLException {
    return switch (idScheme.getIdScheme()) {
      case UID -> idScheme.toMetadataIdentifier(rs.getString("uid"));
      case CODE -> idScheme.toMetadataIdentifier(rs.getString("code"));
      case NAME -> idScheme.toMetadataIdentifier(rs.getString("name"));
      case ATTRIBUTE -> {
        String attributeValues = rs.getString("attributevalues");
        yield idScheme.toMetadataIdentifier(
            attributeValues == null
                ? null
                : AttributeValues.of(attributeValues).get(idScheme.getAttributeUid()));
      }
    };
  }
}
