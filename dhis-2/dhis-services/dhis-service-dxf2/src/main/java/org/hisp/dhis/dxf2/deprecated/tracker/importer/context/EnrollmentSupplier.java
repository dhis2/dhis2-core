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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.context;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Event;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component("workContextProgramInstancesSupplier")
public class EnrollmentSupplier extends AbstractSupplier<Map<String, Enrollment>> {
  private final ProgramSupplier programSupplier;

  public EnrollmentSupplier(
      NamedParameterJdbcTemplate jdbcTemplate, ProgramSupplier programSupplier) {
    super(jdbcTemplate);
    this.programSupplier = programSupplier;
  }

  public Map<String, Enrollment> get(
      ImportOptions importOptions,
      Map<String, Pair<TrackedEntity, Boolean>> teiMap,
      List<Event> events) {
    if (events == null) {
      return new HashMap<>();
    }

    // Collect all the enrollment UIDs to pass as SQL query argument
    Set<String> programInstanceUids =
        events.stream()
            .map(Event::getEnrollment)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toSet());

    Map<String, Enrollment> programInstances = new HashMap<>();

    if (!programInstanceUids.isEmpty()) {
      // Create a bi-directional map enrollment uid -> event uid
      Multimap<String, String> programInstanceToEvent = HashMultimap.create();
      for (Event event : events) {
        programInstanceToEvent.put(event.getEnrollment(), event.getUid());
      }

      // Collect all the Program Stage Instances specified in the Events
      // (enrollment
      // property)
      programInstances =
          getProgramInstancesByUid(
              importOptions, events, programInstanceToEvent, programInstanceUids);
    }

    mapExistingEventsToProgramInstances(importOptions, events, programInstances);

    mapEventsToProgramInstanceByTei(importOptions, events, programInstances, teiMap);

    return programInstances;
  }

  /**
   * Loop through the events and check if there is any event left without an enrollment: for each
   * Event without an enrollment, try to fetch the enrollment by Program and Tracked Entity Instance
   */
  private void mapEventsToProgramInstanceByTei(
      ImportOptions importOptions,
      List<Event> events,
      Map<String, Enrollment> programInstances,
      Map<String, Pair<TrackedEntity, Boolean>> teiMap) {
    for (Event event : events) {
      if (!programInstances.containsKey(event.getUid())) {
        Pair<TrackedEntity, Boolean> teiPair = teiMap.get(event.getUid());
        Program program =
            getProgramByUid(
                event.getProgram(), programSupplier.get(importOptions, events).values());
        if (teiPair != null && program != null) {
          TrackedEntity tei = teiPair.getKey();

          Enrollment enrollment =
              getByTeiAndProgram(importOptions, tei.getId(), program.getId(), event);
          if (enrollment != null) {
            programInstances.put(event.getUid(), enrollment);
          }
        }
      }
    }
  }

  /**
   * This method is only used if the Event already exist in the db (update) If the Event does not
   * have the "enrollment" property set OR enrollment is pointing to an invalid UID, use the
   * Enrollment already connected to the Event.
   */
  private void mapExistingEventsToProgramInstances(
      ImportOptions importOptions, List<Event> events, Map<String, Enrollment> programInstances) {
    // Collect all the Enrollments by event uid
    final Map<String, Enrollment> programInstancesByEvent =
        getProgramInstanceByEvent(importOptions, events);

    if (!programInstancesByEvent.isEmpty()) {
      for (Event event : events) {
        if (!programInstances.containsKey(event.getUid())) {
          if (programInstancesByEvent.containsKey(event.getUid())) {
            programInstances.put(event.getUid(), programInstancesByEvent.get(event.getUid()));
          }
        }
      }
    }
  }

  private Program getProgramById(long id, Collection<Program> programs) {
    return programs.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
  }

  private Program getProgramByUid(String uid, Collection<Program> programs) {
    return programs.stream().filter(p -> p.getUid().equals(uid)).findFirst().orElse(null);
  }

  private Enrollment getByTeiAndProgram(
      ImportOptions importOptions, Long teiId, Long programId, Event event) {
    final String sql =
        "select pi.enrollmentid, pi.programid, pi.uid , t.trackedentityid as tei_id, t.uid as tei_uid, "
            + "ou.uid as tei_ou_uid, ou.path as tei_ou_path from enrollment pi "
            + "join trackedentity t on t.trackedentityid = pi.trackedentityid "
            + "join organisationunit ou on t.organisationunitid = ou.organisationunitid "
            + "where pi.programid = :programid "
            + "and pi.status = 'ACTIVE' "
            + "and pi.trackedentityid = :teiid";

    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("programid", programId);
    parameters.addValue("teiid", teiId);

    List<Enrollment> query =
        jdbcTemplate.query(
            sql,
            parameters,
            (ResultSet rs) -> {
              List<Enrollment> results = new ArrayList<>();

              while (rs.next()) {
                results.add(mapFromResultset(rs, importOptions, Collections.singletonList(event)));
              }
              return results;
            });

    if (query != null && query.size() == 1) {
      return query.get(0);
    } else {
      return null;
    }
  }

  private Map<String, Enrollment> getProgramInstanceByEvent(
      ImportOptions importOptions, List<Event> events) {
    final Set<String> eventUids = events.stream().map(Event::getUid).collect(Collectors.toSet());
    if (isEmpty(eventUids)) {
      return new HashMap<>();
    }

    final String sql =
        "select psi.uid as psi_uid, pi.enrollmentid, pi.programid, pi.uid , t.trackedentityid as tei_id, t.uid as tei_uid, "
            + "ou.uid as tei_ou_uid, ou.path as tei_ou_path from enrollment pi "
            + "left outer join trackedentity t on pi.trackedentityid = t.trackedentityid "
            + "left join organisationunit ou on t.organisationunitid = ou.organisationunitid "
            + "join event psi on pi.enrollmentid = psi.enrollmentid "
            + "where psi.uid in (:ids)";

    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("ids", eventUids);

    return jdbcTemplate.query(
        sql,
        parameters,
        (ResultSet rs) -> {
          Map<String, Enrollment> results = new HashMap<>();

          while (rs.next()) {
            results.put(rs.getString("psi_uid"), mapFromResultset(rs, importOptions, events));
          }
          return results;
        });
  }

  private Map<String, Enrollment> getProgramInstancesByUid(
      ImportOptions importOptions,
      List<Event> events,
      Multimap<String, String> programInstanceToEvent,
      Set<String> uids) {

    final String sql =
        "select pi.enrollmentid, pi.programid, pi.uid, t.trackedentityid as tei_id, t.uid as tei_uid, "
            + "ou.uid as tei_ou_uid, ou.path as tei_ou_path "
            + "from enrollment pi join trackedentity t on pi.trackedentityid = t.trackedentityid "
            + "join organisationunit ou on t.organisationunitid = ou.organisationunitid where pi.uid in (:ids)";

    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("ids", uids);

    return jdbcTemplate.query(
        sql,
        parameters,
        (ResultSet rs) -> {
          Map<String, Enrollment> results = new HashMap<>();

          while (rs.next()) {
            Enrollment pi = mapFromResultset(rs, importOptions, events);

            for (String event : programInstanceToEvent.get(pi.getUid())) {
              results.put(event, pi);
            }
          }
          return results;
        });
  }

  private Enrollment mapFromResultset(ResultSet rs, ImportOptions importOptions, List<Event> events)
      throws SQLException {
    Enrollment pi = new Enrollment();
    pi.setId(rs.getLong("enrollmentid"));
    pi.setUid(rs.getString("uid"));
    pi.setProgram(
        getProgramById(
            rs.getLong("programid"), programSupplier.get(importOptions, events).values()));

    String teiUid = rs.getString("tei_uid");

    if (teiUid != null) {
      TrackedEntity trackedEntity = new TrackedEntity();
      trackedEntity.setId(rs.getLong("tei_id"));
      String teiOuUid = rs.getString("tei_ou_uid");
      if (teiOuUid != null) {
        OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setUid(teiOuUid);
        organisationUnit.setParent(
            SupplierUtils.getParentHierarchy(organisationUnit, rs.getString("tei_ou_path")));
        trackedEntity.setOrganisationUnit(organisationUnit);
      }
      trackedEntity.setUid(teiUid);

      pi.setTrackedEntity(trackedEntity);
    }

    return pi;
  }

  @Override
  public Map<String, Enrollment> get(ImportOptions importOptions, List<Event> events) {
    throw new NotImplementedException("Use other get method");
  }
}
