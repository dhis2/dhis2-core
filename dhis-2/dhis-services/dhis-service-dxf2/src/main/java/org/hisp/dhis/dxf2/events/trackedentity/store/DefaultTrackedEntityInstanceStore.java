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
import java.util.Map;

import org.hisp.dhis.dxf2.events.aggregates.AggregateContext;
import org.hisp.dhis.dxf2.events.trackedentity.Attribute;
import org.hisp.dhis.dxf2.events.trackedentity.ProgramOwner;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.store.mapper.ProgramOwnerRowCallbackHandler;
import org.hisp.dhis.dxf2.events.trackedentity.store.mapper.TrackedEntityAttributeRowCallbackHandler;
import org.hisp.dhis.dxf2.events.trackedentity.store.mapper.TrackedEntityInstanceRowCallbackHandler;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
@Repository
public class DefaultTrackedEntityInstanceStore
    extends
    AbstractStore
    implements
    TrackedEntityInstanceStore
{
    private final static String GET_TEI_ACL_CHECK = "tei.trackedentitytypeid = (SELECT distinct TETUGA.trackedentitytypeid"
        + "                                 FROM trackedentitytypeusergroupaccesses TETUGA"
        + "                                          LEFT JOIN usergroupaccess UGA on TETUGA.usergroupaccessid = UGA.usergroupaccessid"
        + "                                          LEFT JOIN usergroupmembers UGM on UGA.usergroupid = UGM.usergroupid,"
        + "                                      trackedentitytypeuseraccesses TETUA"
        + "                                          LEFT JOIN useraccess UA on TETUA.useraccessid = UA.useraccessid"
        + "                                 WHERE UGM.userid = :userId"
        + "                                   AND UA.userid = :userId"
        + "                                   AND UGA.access LIKE '__r_____')";

    private final static String GET_TEIS_SQL = "SELECT tei.uid as teiuid"
        + ", tei.created, " + "tei.createdatclient, tei.lastupdated, tei.lastupdatedatclient, tei.inactive, "
        + "       tei.deleted, tei.geometry, tet.uid as type_uid, o.uid   as ou_uid "
        + "FROM trackedentityinstance tei "
        + "         join trackedentitytype tet on tei.trackedentitytypeid = tet.trackedentitytypeid "
        + "         join organisationunit o on tei.organisationunitid = o.organisationunitid where tei.trackedentityinstanceid in (:ids) ";


    private final static String GET_TEI_ATTRIBUTES = "select tei.uid as teiuid"
        + ", teav.trackedentityinstanceid as id, teav.created, teav.lastupdated, "
        + "       teav.value, teav.storedby, t.name as att_name, "
        + "       t.uid as att_uid, t.valuetype as att_val_type, "
        + "       t.code as att_code, t.skipsynchronization as att_skip_sync "
        + "from trackedentityattributevalue teav "
        + "         join trackedentityattribute t on teav.trackedentityattributeid = t.trackedentityattributeid join trackedentityinstance tei on teav.trackedentityinstanceid = tei.trackedentityinstanceid "
        + "where teav.trackedentityinstanceid in (:ids)";

    private final static String GET_PROGRAM_OWNERS = "select tei.uid as key, p.uid as prguid, o.uid as ouuid "
        +
            "from trackedentityprogramowner teop " +
            "         join program p on teop.programid = p.programid " +
            "         join organisationunit o on teop.organisationunitid = o.organisationunitid " +
            "         join trackedentityinstance tei on teop.trackedentityinstanceid = tei.trackedentityinstanceid " +
            "where teop.trackedentityinstanceid in (:ids)";

    public DefaultTrackedEntityInstanceStore( @Qualifier( "readOnlyJdbcTemplate" ) JdbcTemplate jdbcTemplate )
    {
        super( jdbcTemplate );
    }

    @Override
    String getRelationshipEntityColumn() {
        return "trackedentityinstanceid";
    }

    @Override
    public Map<String, TrackedEntityInstance> getTrackedEntityInstances( List<Long> ids, AggregateContext ctx )
    {
        TrackedEntityInstanceRowCallbackHandler handler = new TrackedEntityInstanceRowCallbackHandler();
        jdbcTemplate.query( withAclCheck( GET_TEIS_SQL, ctx, GET_TEI_ACL_CHECK ),
            createIdsParam( ids, ctx.getUserId() ), handler );
        return handler.getItems();
    }

    @Override
    public Multimap<String, Attribute> getAttributes( List<Long> ids )
    {
        TrackedEntityAttributeRowCallbackHandler handler = new TrackedEntityAttributeRowCallbackHandler();
        jdbcTemplate.query( GET_TEI_ATTRIBUTES, createIdsParam( ids ), handler );
        return handler.getItems();
    }

    public Multimap<String, ProgramOwner> getProgramOwners( List<Long> ids )
    {
        ProgramOwnerRowCallbackHandler handler = new ProgramOwnerRowCallbackHandler();
        jdbcTemplate.query( GET_PROGRAM_OWNERS, createIdsParam( ids ), handler );
        return handler.getItems();

    }
}
