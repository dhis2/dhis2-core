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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Event;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.sharing.Sharing;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * This supplier builds and caches a Map of all the Programs in the system. For each Program, the
 * following additional data is retrieved:
 *
 * @formatter:off
 *     <p>Program + | +---+ Program Stage (List) | | | +---+ User Access (ACL) | | | +---+ User
 *     Group Access (ACL) | | +---+ Category Combo | | +---+ Tracked Entity Instance | | | +---+
 *     User Access (ACL) | | | +---+ User Group Access (ACL) | | | +---+ Organizational Unit (List)
 *     | +---+ User Access (ACL) | +---+ User Group Access (ACL)
 * @formatter:on
 * @author Luciano Fiandesio
 */
@Component("workContextProgramsSupplier")
public class ProgramSupplier extends AbstractSupplier<Map<String, Program>> {
  private static final String PROGRAM_CACHE_KEY = "000P";

  private static final String ATTRIBUTESCHEME_COL = "attributevalues";

  private static final String PROGRAM_STAGE_ID = "programstageid";

  private static final String COMPULSORY = "compulsory";

  private static final String TRACKED_ENTITY_TYPE_ID = "trackedentitytypeid";

  // Caches the entire program hierarchy, including program stages and ACL
  private final Cache<Map<String, Program>> programsCache;

  public ProgramSupplier(NamedParameterJdbcTemplate jdbcTemplate, CacheProvider cacheProvider) {
    super(jdbcTemplate);
    programsCache = cacheProvider.createProgramCache();
  }

  @Override
  public Map<String, Program> get(ImportOptions importOptions, List<Event> eventList) {
    boolean requiresReload = false;

    // Do not use cache if {@code skipCache} is true or if running as test

    if (importOptions.isSkipCache()) {
      programsCache.invalidateAll();
    }
    Map<String, Program> programMap = programsCache.get(PROGRAM_CACHE_KEY).orElse(null);

    if (requiresCacheReload(eventList, programMap)) {
      programsCache.invalidateAll();
      requiresReload = true;
    }

    if (requiresReload || programMap == null) {
      // Load all programs

      programMap = loadPrograms(importOptions.getIdSchemes());

      if (!MapUtils.isEmpty(programMap)) {
        aggregateProgramAndAclData(programMap, loadProgramStageDataElementSets());

        programsCache.put(PROGRAM_CACHE_KEY, programMap);
      } else {
        programsCache.put(PROGRAM_CACHE_KEY, new HashMap<>());
      }
    }

    return programMap;
  }

  private void aggregateProgramAndAclData(
      Map<String, Program> programMap, Map<Long, DataElementSets> dataElementSetsMap) {
    for (Program program : programMap.values()) {
      for (ProgramStage programStage : program.getProgramStages()) {
        DataElementSets dataElementSets = dataElementSetsMap.get(programStage.getId());
        if (dataElementSets != null) {
          Stream<ProgramStageDataElement> compulsoryProgramStageDataElementsStream =
              getOrEmpty(dataElementSets.getCompulsoryDataElements())
                  .map(de -> new ProgramStageDataElement(programStage, de, true));

          Stream<ProgramStageDataElement> nonCompulsoryProgramStageDataElementsStream =
              getOrEmpty(dataElementSets.getNonCompulsoryDataElements())
                  .map(de -> new ProgramStageDataElement(programStage, de, false));

          Set<ProgramStageDataElement> allProgramStageDataElements =
              Stream.concat(
                      compulsoryProgramStageDataElementsStream,
                      nonCompulsoryProgramStageDataElementsStream)
                  .collect(Collectors.toSet());

          programStage.setProgramStageDataElements(allProgramStageDataElements);
        }
      }
    }
  }

  private Stream<DataElement> getOrEmpty(Set<DataElement> dataElementSet) {
    return Optional.ofNullable(dataElementSet).orElse(Collections.emptySet()).stream();
  }

  /**
   * Loads all data elements for each program stage, partitioned into compulsory and non-compulsory.
   */
  private Map<Long, DataElementSets> loadProgramStageDataElementSets() {
    final String sql =
        "select psde.programstageid, de.dataelementid, de.uid as de_uid, de.code as de_code, psde.compulsory "
            + "from programstagedataelement psde "
            + "join dataelement de on psde.dataelementid = de.dataelementid "
            + "order by psde.programstageid";

    return jdbcTemplate.query(
        sql,
        (ResultSet rs) -> {
          Map<Long, DataElementSets> results = new HashMap<>();
          long programStageId = 0;
          while (rs.next()) {
            if (programStageId != rs.getLong(PROGRAM_STAGE_ID)) {
              DataElementSets dataElementSets = new DataElementSets();

              DataElement dataElement = toDataElement(rs);

              if (rs.getBoolean(COMPULSORY)) {
                dataElementSets.getCompulsoryDataElements().add(dataElement);
              } else {
                dataElementSets.getNonCompulsoryDataElements().add(dataElement);
              }

              results.put(rs.getLong(PROGRAM_STAGE_ID), dataElementSets);
              programStageId = rs.getLong(PROGRAM_STAGE_ID);
            } else {
              DataElementSets dataElementSets = results.get(rs.getLong(PROGRAM_STAGE_ID));
              DataElement dataElement = toDataElement(rs);
              if (rs.getBoolean(COMPULSORY)) {
                dataElementSets.getCompulsoryDataElements().add(dataElement);
              } else {
                dataElementSets.getNonCompulsoryDataElements().add(dataElement);
              }
            }
          }
          return results;
        });
  }

  @Data
  static class DataElementSets {
    private final Set<DataElement> compulsoryDataElements = new HashSet<>();

    private final Set<DataElement> nonCompulsoryDataElements = new HashSet<>();
  }

  private Map<String, Program> loadPrograms(IdSchemes idSchemes) {
    // Get Id scheme for programs, programs should support also the
    // attribute scheme based on JSONB

    IdScheme idScheme = idSchemes.getProgramIdScheme();

    String sqlSelect =
        "select p.programid as id, p.uid, p.code, p.name, p.sharing as program_sharing, "
            + "p.type, p.opendaysaftercoenddate, tet.trackedentitytypeid, tet.sharing  as tet_sharing, "
            + "tet.uid           as tet_uid, c.categorycomboid as catcombo_id, "
            + "c.uid             as catcombo_uid, c.name            as catcombo_name, "
            + "c.code            as catcombo_code, ps.programstageid as ps_id, ps.uid as ps_uid, "
            + "ps.code           as ps_code, ps.name           as ps_name, "
            + "ps.featuretype    as ps_feature_type, ps.sort_order, ps.sharing   as ps_sharing, "
            + "ps.repeatable     as ps_repeatable, ps.enableuserassignment, ps.validationstrategy";

    if (idScheme.isAttribute()) {
      sqlSelect +=
          ",p.attributevalues->'"
              + idScheme.getAttribute()
              + "'->>'value' as "
              + ATTRIBUTESCHEME_COL;
    }

    final String sql =
        sqlSelect
            + " from program p "
            + "LEFT JOIN categorycombo c on p.categorycomboid = c.categorycomboid "
            + "LEFT JOIN trackedentitytype tet on p.trackedentitytypeid = tet.trackedentitytypeid "
            + "LEFT JOIN programstage ps on p.programid = ps.programid "
            + "LEFT JOIN program_organisationunits pou on p.programid = pou.programid "
            + "LEFT JOIN organisationunit ou on pou.organisationunitid = ou.organisationunitid "
            + "group by p.programid, tet.trackedentitytypeid, c.categorycomboid, ps.programstageid, ps.sort_order "
            + "order by p.programid, ps.sort_order";

    return jdbcTemplate.query(
        sql,
        (ResultSet rs) -> {
          Map<String, Program> results = new HashMap<>();
          long programId = 0;
          while (rs.next()) {
            if (programId != rs.getLong("id")) {
              Set<ProgramStage> programStages = new HashSet<>();
              Program program = new Program();

              program.setId(rs.getLong("id"));
              program.setUid(rs.getString("uid"));
              program.setName(rs.getString("name"));
              program.setCode(rs.getString("code"));
              program.setOpenDaysAfterCoEndDate(rs.getInt("opendaysaftercoenddate"));

              program.setProgramType(ProgramType.fromValue(rs.getString("type")));
              program.setSharing(toSharing(rs.getObject("program_sharing").toString()));

              // Do not add program stages without primary key (this
              // should not really happen, but the database does allow
              // Program Stage without a Program Id
              if (rs.getLong("ps_id") != 0) {
                programStages.add(toProgramStage(rs));
              }

              CategoryCombo categoryCombo = new CategoryCombo();
              categoryCombo.setId(rs.getLong("catcombo_id"));
              categoryCombo.setUid(rs.getString("catcombo_uid"));
              categoryCombo.setName(rs.getString("catcombo_name"));
              categoryCombo.setCode(rs.getString("catcombo_code"));
              program.setCategoryCombo(categoryCombo);

              long tetId = rs.getLong(TRACKED_ENTITY_TYPE_ID);
              if (tetId != 0) {
                TrackedEntityType trackedEntityType = new TrackedEntityType();
                trackedEntityType.setId(tetId);
                trackedEntityType.setUid(rs.getString("tet_uid"));
                trackedEntityType.setSharing(toSharing(rs.getObject("tet_sharing").toString()));
                program.setTrackedEntityType(trackedEntityType);
              }

              program.setProgramStages(programStages);
              results.put(getProgramKey(idScheme, rs), program);

              programId = program.getId();
            } else {
              results.get(getProgramKey(idScheme, rs)).getProgramStages().add(toProgramStage(rs));
            }
          }
          return results;
        });
  }

  /**
   * Resolve the key to place in the Program Map, based on the Scheme specified in the request If
   * the scheme is of type Attribute, use the attribute value from the JSONB column
   */
  private String getProgramKey(IdScheme programIdScheme, ResultSet rs) throws SQLException {
    return programIdScheme.isAttribute()
        ? rs.getString(ATTRIBUTESCHEME_COL)
        : IdSchemeUtils.getKey(programIdScheme, rs);
  }

  private ProgramStage toProgramStage(ResultSet rs) throws SQLException {
    ProgramStage programStage = new ProgramStage();
    programStage.setId(rs.getLong("ps_id"));
    programStage.setUid(rs.getString("ps_uid"));
    programStage.setCode(rs.getString("ps_code"));
    programStage.setName(rs.getString("ps_name"));
    programStage.setSortOrder(rs.getInt("sort_order"));
    programStage.setSharing(toSharing(rs.getObject("ps_sharing").toString()));
    programStage.setFeatureType(
        rs.getString("ps_feature_type") != null
            ? FeatureType.valueOf(rs.getString("ps_feature_type"))
            : FeatureType.NONE);
    programStage.setRepeatable(rs.getBoolean("ps_repeatable"));
    programStage.setEnableUserAssignment(rs.getBoolean("enableuserassignment"));
    String validationStrategy = rs.getString("validationstrategy");
    if (StringUtils.isNotEmpty(validationStrategy)) {
      programStage.setValidationStrategy(ValidationStrategy.valueOf(validationStrategy));
    }
    return programStage;
  }

  private DataElement toDataElement(ResultSet rs) throws SQLException {
    DataElement dataElement = new DataElement();
    dataElement.setId(rs.getLong("dataelementid"));
    dataElement.setUid(rs.getString("de_uid"));
    dataElement.setCode(rs.getString("de_code"));
    return dataElement;
  }

  /**
   * Check if the list of incoming Events contains one or more Program uid which is not in cache.
   * Reload the entire program cache if a Program UID is not found
   */
  private boolean requiresCacheReload(List<Event> events, Map<String, Program> programMap) {
    if (programMap == null) {
      return false;
    }
    Set<String> programs = events.stream().map(Event::getProgram).collect(Collectors.toSet());

    final Set<String> programsInCache = programMap.keySet();

    for (String program : programs) {
      if (!programsInCache.contains(program)) {
        return true;
      }
    }
    return false;
  }

  private Sharing toSharing(String json) {
    if (StringUtils.isEmpty(json)) return null;

    try {
      return JacksonObjectMapperConfig.staticJsonMapper().readValue(json, Sharing.class);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }

    return null;
  }
}
