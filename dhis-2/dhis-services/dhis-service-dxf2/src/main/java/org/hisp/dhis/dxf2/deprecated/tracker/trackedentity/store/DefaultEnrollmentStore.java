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
package org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.List;
import org.hisp.dhis.dxf2.deprecated.tracker.aggregates.AggregateContext;
import org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Note;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.Attribute;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.mapper.EnrollmentRowCallbackHandler;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.mapper.NoteRowCallbackHandler;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.mapper.ProgramAttributeRowCallbackHandler;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.query.EnrollmentQuery;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.query.ProgramAttributeQuery;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Luciano Fiandesio
 */
@Repository
public class DefaultEnrollmentStore extends AbstractStore implements EnrollmentStore {
  private static final String GET_ENROLLMENT_SQL_BY_TEI = EnrollmentQuery.getQuery();

  private static final String GET_ATTRIBUTES = ProgramAttributeQuery.getQuery();

  private static final String GET_NOTES_SQL =
      "select pi.uid as key, tec.uid, tec.commenttext, "
          + "tec.creator, tec.created "
          + "from trackedentitycomment tec join programinstancecomments pic "
          + "on tec.trackedentitycommentid = pic.trackedentitycommentid "
          + "join enrollment pi on pic.programinstanceid = pi.enrollmentid "
          + "where pic.programinstanceid in (:ids)";

  private static final String FILTER_OUT_DELETED_ENROLLMENTS = "pi.deleted=false";

  public DefaultEnrollmentStore(@Qualifier("readOnlyJdbcTemplate") JdbcTemplate jdbcTemplate) {
    super(jdbcTemplate);
  }

  public Multimap<String, Enrollment> getEnrollmentsByTrackedEntityInstanceIds(
      List<Long> ids, AggregateContext ctx) {
    List<List<Long>> teiIds = Lists.partition(ids, PARITITION_SIZE);

    Multimap<String, Enrollment> enrollmentMultimap = ArrayListMultimap.create();

    teiIds.forEach(
        partition ->
            enrollmentMultimap.putAll(
                getEnrollmentsByTrackedEntityInstanceIdsPartitioned(partition, ctx)));

    return enrollmentMultimap;
  }

  private Multimap<String, Enrollment> getEnrollmentsByTrackedEntityInstanceIdsPartitioned(
      List<Long> ids, AggregateContext ctx) {
    EnrollmentRowCallbackHandler handler = new EnrollmentRowCallbackHandler();

    jdbcTemplate.query(
        getQuery(
            GET_ENROLLMENT_SQL_BY_TEI,
            ctx,
            " pi.programid IN (:programIds)",
            FILTER_OUT_DELETED_ENROLLMENTS),
        createIdsParam(ids).addValue("programIds", ctx.getPrograms()),
        handler);

    return handler.getItems();
  }

  @Override
  public Multimap<String, Note> getNotes(List<Long> ids) {
    return fetch(GET_NOTES_SQL, new NoteRowCallbackHandler(), ids);
  }

  @Override
  public Multimap<String, Attribute> getAttributes(List<Long> ids, AggregateContext ctx) {
    ProgramAttributeRowCallbackHandler handler = new ProgramAttributeRowCallbackHandler();

    jdbcTemplate.query(
        getQuery(
            GET_ATTRIBUTES, ctx, " pa.programid IN (:programIds)", FILTER_OUT_DELETED_ENROLLMENTS),
        createIdsParam(ids).addValue("programIds", ctx.getPrograms()),
        handler);

    return handler.getItems();
  }

  @Override
  String getRelationshipEntityColumn() {
    return "programinstanceid";
  }
}
