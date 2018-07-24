package org.hisp.dhis.trackedentitydatavalue.hibernate;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hibernate.Criteria;
import org.hibernate.query.Query;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Abyot Asalefew Gizaw
 */
public class HibernateTrackedEntityDataValueStore
    extends HibernateGenericStore<TrackedEntityDataValue>
    implements TrackedEntityDataValueStore
{
    @Autowired
    DataElementStore dataElementStore;

    @Resource( name = "readOnlyJdbcTemplate" )
    private JdbcTemplate jdbcTemplate;

    @Override
    public void saveVoid( TrackedEntityDataValue dataValue )
    {
        sessionFactory.getCurrentSession().save( dataValue );
    }

    @Override
    public int delete( ProgramStageInstance programStageInstance )
    {
        Query query = getQuery( "delete from TrackedEntityDataValue where programStageInstance = :programStageInstance" );
        query.setParameter( "programStageInstance", programStageInstance );
        return query.executeUpdate();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityDataValue> get( ProgramStageInstance programStageInstance )
    {
        String hql = "from TrackedEntityDataValue tv where tv.programStageInstance =:programStageInstance";

        return getList( getQuery( hql ).setParameter( "programStageInstance", programStageInstance ) );
    }

    @Override
    public List<TrackedEntityDataValue> getTrackedEntityDataValuesForSynchronization( ProgramStageInstance programStageInstance )
    {
        List<TrackedEntityDataValue> dataValues = new ArrayList<>();

        String sql = "SELECT tedv.* FROM trackedentitydatavalue tedv " +
            "LEFT JOIN programstageinstance psi on tedv.programstageinstanceid = psi.programstageinstanceid " +
            "LEFT JOIN programstagedataelement psde ON tedv.dataelementid = psde.dataelementid AND psi.programstageid = psde.programstageid " +
            "WHERE tedv.programstageinstanceid = " + programStageInstance.getId() + " AND psde.skipsynchronization = false";

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        while ( rowSet.next() )
        {
            TrackedEntityDataValue tedv = new TrackedEntityDataValue();

            tedv.setCreated( rowSet.getDate( "created" ) );
            tedv.setLastUpdated( rowSet.getDate( "lastupdated" ) );
            tedv.setProgramStageInstance( programStageInstance );
            tedv.setValue( rowSet.getString( "value" ) );
            tedv.setStoredBy( rowSet.getString( "storedby" ) );
            tedv.setProvidedElsewhere( rowSet.getBoolean( "providedelsewhere" ) );
            tedv.setDataElement( dataElementStore.get( rowSet.getInt( "dataelementid" ) ) );

            dataValues.add( tedv );
        }

        return dataValues;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityDataValue> get( ProgramStageInstance programStageInstance,
        Collection<DataElement> dataElements )
    {
        if ( dataElements == null || dataElements.isEmpty() )
        {
            return new ArrayList<>();
        }

        String hql = "from TrackedEntityDataValue tv where tv.dataElement in :dataElements and tv.programStageInstance =:programStageInstance";
        return getList( getQuery( hql )
            .setParameter( "dataElements", dataElements )
            .setParameter( "programStageInstance", programStageInstance ) );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityDataValue> get( Collection<ProgramStageInstance> programStageInstances )
    {
        if ( programStageInstances == null || programStageInstances.isEmpty() )
        {
            return new ArrayList<>();
        }

        String hql = "from TrackedEntityDataValue tv where tv.programStageInstance in :programStageInstances";
        return getList( getQuery( hql ).setParameter( "programStageInstances", programStageInstances ) );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityDataValue> get( DataElement dataElement )
    {
        String hql = "from TrackedEntityDataValue tv where tv.dataElement =:dataElement";
        return getList( getQuery( hql ).setParameter( "dataElement", dataElement ) );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityDataValue> get( TrackedEntityInstance entityInstance, Collection<DataElement> dataElements, Date startDate,
        Date endDate )
    {
        if ( dataElements == null || dataElements.isEmpty() )
        {
            return new ArrayList<>();
        }

        Criteria criteria = getCriteria();
        criteria.createAlias( "programStageInstance", "programStageInstance" );
        criteria.createAlias( "programStageInstance.programInstance", "programInstance" );
        criteria.add( Restrictions.in( "dataElement", dataElements ) );
        criteria.add( Restrictions.eq( "programInstance.entityInstance", entityInstance ) );
        criteria.add( Restrictions.between( "programStageInstance.executionDate", startDate, endDate ) );

        return criteria.list();
    }

    @Override
    public TrackedEntityDataValue get( ProgramStageInstance programStageInstance, DataElement dataElement )
    {
        String hql = "from TrackedEntityDataValue tv where tv.programStageInstance =:programStageInstance " +
            "and tv.dataElement =:dataElement";

        return getSingleResult( getQuery( hql )
            .setParameter( "programStageInstance", programStageInstance )
            .setParameter( "dataElement", dataElement ) );
    }

}
