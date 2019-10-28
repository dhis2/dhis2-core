/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.dxf2.events.trackedentity.store;

import java.util.List;

import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.trackedentity.store.mapper.AbstractMapper;
import org.hisp.dhis.dxf2.events.trackedentity.store.mapper.EnrollmentRowCallbackHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
@Repository
public class DefaultEnrollmentStore
    extends
    AbstractStore
    implements
    EnrollmentStore
{

    private final static String GET_ENROLLMENT_SQL = "select tei.uid as teiuid,  pi.programinstanceid, pi.uid, "
        + "       pi.created, pi.createdatclient, pi.lastupdated, pi.lastupdatedatclient, pi.status, "
        + "       pi.enrollmentdate, pi.incidentdate, pi.followup, pi.enddate, pi.completedby, "
        + "       pi.storedby, pi.deleted, pi.geometry, p.uid as program_uid, "
        + "       p.featuretype as program_feature_type from programinstance pi "
        + "         join program p on pi.programid = p.programid "
        + "join trackedentityinstance tei on pi.trackedentityinstanceid = tei.trackedentityinstanceid "
        + "where pi.trackedentityinstanceid in (:ids)";

    public DefaultEnrollmentStore( @Qualifier( "readOnlyJdbcTemplate" ) JdbcTemplate jdbcTemplate )
    {
        super( jdbcTemplate );
    }

    public Multimap<String, Enrollment> getEnrollments( List<Long> ids )
    {
        EnrollmentRowCallbackHandler handler = new EnrollmentRowCallbackHandler();
        jdbcTemplate.query( GET_ENROLLMENT_SQL, createIdsParam( ids ), handler );
        return handler.getItems();
    }
}
