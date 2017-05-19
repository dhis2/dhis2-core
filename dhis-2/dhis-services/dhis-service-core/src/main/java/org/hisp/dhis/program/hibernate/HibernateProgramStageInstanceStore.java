package org.hisp.dhis.program.hibernate;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.time.DateUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author Abyot Asalefew
 */
public class HibernateProgramStageInstanceStore
    extends HibernateIdentifiableObjectStore<ProgramStageInstance>
    implements ProgramStageInstanceStore
{
    private final static Set<NotificationTrigger> SCHEDULED_PROGRAM_STAGE_INSTANCE_TRIGGERS =
        Sets.intersection(
            NotificationTrigger.getAllApplicableToProgramStageInstance(),
            NotificationTrigger.getAllScheduledTriggers()
        );

    @Override
    @SuppressWarnings( "unchecked" )
    public ProgramStageInstance get( ProgramInstance programInstance, ProgramStage programStage )
    {
        List<ProgramStageInstance> list = getCriteria(
            Restrictions.eq( "programInstance", programInstance ),
            Restrictions.eq( "programStage", programStage ) ).
            addOrder( Order.asc( "id" ) ).list();

        return list.isEmpty() ? null : list.get( list.size() - 1 );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ProgramStageInstance> get( Collection<ProgramInstance> programInstances, EventStatus status )
    {
        return getCriteria(
            Restrictions.in( "programInstance", programInstances ),
            Restrictions.eq( "status", status ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ProgramStageInstance> get( TrackedEntityInstance entityInstance, EventStatus status )
    {
        Criteria criteria = getCriteria();
        criteria.createAlias( "programInstance", "programInstance" );
        criteria.add( Restrictions.eq( "programInstance.entityInstance", entityInstance ) );
        criteria.add( Restrictions.eq( "status", status ) );
        return criteria.list();
    }



    @Override
    public int count( ProgramStage programStage, Collection<Integer> orgunitIds, Date startDate, Date endDate,
        Boolean completed )
    {
        Number rs = (Number) getCriteria( programStage, orgunitIds, startDate, endDate, completed ).setProjection(
            Projections.rowCount() ).uniqueResult();

        return rs != null ? rs.intValue() : 0;
    }

    @Override
    public long getProgramStageInstanceCountLastUpdatedAfter( Date time )
    {
        Number rs = (Number) getCriteria()
            .add( Restrictions.ge( "lastUpdated", time ) )
            .setProjection( Projections.rowCount() )
            .uniqueResult();

        return rs != null ? rs.longValue() : 0;
    }

    @Override
    public boolean exists( String uid )
    {
        Integer result = jdbcTemplate.queryForObject( "select count(*) from programstageinstance where uid=? and deleted is false", Integer.class, uid );
        return result != null && result > 0;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public List<ProgramStageInstance> getWithScheduledNotifications( ProgramNotificationTemplate template, Date notificationDate )
    {
        if ( notificationDate == null || !SCHEDULED_PROGRAM_STAGE_INSTANCE_TRIGGERS.contains( template.getNotificationTrigger() ) )
        {
            return Lists.newArrayList();
        }

        if ( template.getRelativeScheduledDays() == null )
        {
            return Lists.newArrayList();
        }

        Date targetDate = DateUtils.addDays( notificationDate, template.getRelativeScheduledDays() * -1 );

        String hql =
            "select distinct psi from ProgramStageInstance as psi " +
            "inner join psi.programStage as ps " +
            "where :notificationTemplate in elements(ps.notificationTemplates) " +
            "and psi.dueDate is not null " +
            "and psi.executionDate is null " +
            "and psi.status != :skippedEventStatus " +
            "and cast(:targetDate as date) = psi.dueDate " +
            "and psi.deleted is false";

        return getQuery( hql )
            .setEntity( "notificationTemplate", template )
            .setString( "skippedEventStatus", EventStatus.SKIPPED.name() )
            .setDate( "targetDate", targetDate ).list();
    }

    @Override
    protected void preProcessDetachedCriteria( DetachedCriteria criteria )
    {
        // Filter out soft deleted values
        criteria.add( Restrictions.eq( "deleted", false ) );
    }

    @Override
    protected ProgramStageInstance postProcessObject( ProgramStageInstance programStageInstance )
    {
        return ( programStageInstance == null || programStageInstance.isDeleted() ) ? null : programStageInstance;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Criteria getCriteria( ProgramStage programStage, Collection<Integer> orgunitIds, Date startDate,
        Date endDate, Boolean completed )
    {
        Criteria criteria = getCriteria();
        criteria.createAlias( "programInstance", "programInstance" );
        criteria.add( Restrictions.eq( "programStage", programStage ) );

        if ( completed == null )
        {
            criteria.createAlias( "programInstance.entityInstance", "entityInstance" );
            criteria.createAlias( "entityInstance.organisationUnit", "regOrgunit" );
            criteria.add( Restrictions.or( Restrictions.and( Restrictions.eq( "status", EventStatus.COMPLETED ),
                    Restrictions.between( "executionDate", startDate, endDate ),
                    Restrictions.in( "organisationUnit.id", orgunitIds ) ), Restrictions.and(
                    Restrictions.eq( "status", EventStatus.ACTIVE ), Restrictions.isNotNull( "executionDate" ),
                    Restrictions.between( "executionDate", startDate, endDate ),
                    Restrictions.in( "organisationUnit.id", orgunitIds ) ),
                Restrictions.and( Restrictions.eq( "status", EventStatus.ACTIVE ), Restrictions.isNull( "executionDate" ),
                    Restrictions.between( "dueDate", startDate, endDate ),
                    Restrictions.in( "regOrgunit.id", orgunitIds ) ), Restrictions.and(
                    Restrictions.eq( "status", EventStatus.SKIPPED ),
                    Restrictions.between( "dueDate", startDate, endDate ),
                    Restrictions.in( "regOrgunit.id", orgunitIds ) ) ) );
        }
        else
        {
            if ( completed )
            {
                criteria.add( Restrictions.and( Restrictions.eq( "status", EventStatus.COMPLETED ),
                    Restrictions.between( "executionDate", startDate, endDate ),
                    Restrictions.in( "organisationUnit.id", orgunitIds ) ) );
            }
            else
            {
                criteria.createAlias( "programInstance.entityInstance", "entityInstance" );
                criteria.createAlias( "entityInstance.organisationUnit", "regOrgunit" );
                criteria.add( Restrictions.and( Restrictions.eq( "status", EventStatus.ACTIVE ),
                    Restrictions.isNotNull( "executionDate" ),
                    Restrictions.between( "executionDate", startDate, endDate ),
                    Restrictions.in( "organisationUnit.id", orgunitIds ) ) );
            }
        }

        return criteria;
    }
}