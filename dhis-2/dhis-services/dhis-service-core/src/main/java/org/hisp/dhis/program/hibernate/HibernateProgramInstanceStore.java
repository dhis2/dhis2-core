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
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.system.util.DateUtils.getMediumDateString;

/**
 * @author Abyot Asalefew
 * @author Lars Helge Overland
 */
public class HibernateProgramInstanceStore
    extends HibernateIdentifiableObjectStore<ProgramInstance>
    implements ProgramInstanceStore
{
    private static final Set<NotificationTrigger> SCHEDULED_PROGRAM_INSTANCE_TRIGGERS =
        Sets.intersection(
            NotificationTrigger.getAllApplicableToProgramInstance(),
            NotificationTrigger.getAllScheduledTriggers()
        );

    @Override
    public int countProgramInstances( ProgramInstanceQueryParams params )
    {
        String hql = buildProgramInstanceHql( params );
        
        Query query = getQuery( hql );

        return ((Number) query.iterate().next()).intValue();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ProgramInstance> getProgramInstances( ProgramInstanceQueryParams params )
    {
        String hql = buildProgramInstanceHql( params );
        
        Query query = getQuery( hql );
        
        if ( params.isPaging() )
        {
            query.setFirstResult( params.getOffset() );
            query.setMaxResults( params.getPageSizeWithDefault() );
        }

        return query.list();
    }

    private String buildProgramInstanceHql( ProgramInstanceQueryParams params )
    {
        String hql = "from ProgramInstance pi";
        SqlHelper hlp = new SqlHelper( true );

        if ( params.hasLastUpdated() )
        {
            hql += hlp.whereAnd() + "pi.lastUpdated >= '" + getMediumDateString( params.getLastUpdated() ) + "'";
        }

        if ( params.hasTrackedEntityInstance() )
        {
            hql += hlp.whereAnd() + "pi.entityInstance.uid = '" + params.getTrackedEntityInstance().getUid() + "'";
        }

        if ( params.hasTrackedEntity() )
        {
            hql += hlp.whereAnd() + "pi.entityInstance.trackedEntity.uid = '" + params.getTrackedEntity().getUid() + "'";
        }

        if ( params.hasOrganisationUnits() )
        {
            if ( params.isOrganisationUnitMode( OrganisationUnitSelectionMode.DESCENDANTS ) )
            {
                String ouClause = "(";
                SqlHelper orHlp = new SqlHelper( true );

                for ( OrganisationUnit organisationUnit : params.getOrganisationUnits() )
                {
                    ouClause += orHlp.or() + "pi.organisationUnit.path LIKE '" + organisationUnit.getPath() + "%'";
                }

                ouClause += ")";

                hql += hlp.whereAnd() + ouClause;
            }
            else
            {
                hql += hlp.whereAnd() + "pi.organisationUnit.uid in (" + getQuotedCommaDelimitedString( getUids( params.getOrganisationUnits() ) ) + ")";
            }
        }

        if ( params.hasProgram() )
        {
            hql += hlp.whereAnd() + "pi.program.uid = '" + params.getProgram().getUid() + "'";
        }

        if ( params.hasProgramStatus() )
        {
            hql += hlp.whereAnd() + "pi.status = '" + params.getProgramStatus() + "'";
        }

        if ( params.hasFollowUp() )
        {
            hql += hlp.whereAnd() + "pi.followup = " + params.getFollowUp();
        }

        if ( params.hasProgramStartDate() )
        {
            hql += hlp.whereAnd() + "pi.enrollmentDate >= '" + getMediumDateString( params.getProgramStartDate() ) + "'";
        }

        if ( params.hasProgramEndDate() )
        {
            hql += hlp.whereAnd() + "pi.enrollmentDate <= '" + getMediumDateString( params.getProgramEndDate() ) + "'";
        }

        return hql;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ProgramInstance> get( Program program )
    {
        return getCriteria( Restrictions.eq( "program", program ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ProgramInstance> get( Program program, ProgramStatus status )
    {
        return getCriteria( Restrictions.eq( "program", program ), Restrictions.eq( "status", status ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ProgramInstance> get( TrackedEntityInstance entityInstance, Program program, ProgramStatus status )
    {
        return getCriteria( Restrictions.eq( "entityInstance", entityInstance ), Restrictions.eq( "program", program ),
            Restrictions.eq( "status", status ) ).list();
    }

    @Override
    public boolean exists( String uid )
    {
        Integer result = jdbcTemplate.queryForObject( "select count(*) from programinstance where uid=?", Integer.class, uid );
        return result != null && result > 0;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public List<ProgramInstance> getWithScheduledNotifications( ProgramNotificationTemplate template, Date notificationDate )
    {
        if ( notificationDate == null || !SCHEDULED_PROGRAM_INSTANCE_TRIGGERS.contains( template.getNotificationTrigger() ) )
        {
            return Lists.newArrayList();
        }

        String dateProperty = toDateProperty( template.getNotificationTrigger() );

        if ( dateProperty == null )
        {
            return Lists.newArrayList();
        }

        Date targetDate = DateUtils.addDays( notificationDate, template.getRelativeScheduledDays() * -1 );

        String hql =
            "select distinct pi from ProgramInstance as pi " +
            "inner join pi.program as p " +
            "where :notificationTemplate in elements(p.notificationTemplates) " +
            "and pi." + dateProperty + " is not null " +
            "and pi.status = :activeEnrollmentStatus " +
            "and cast(:targetDate as date) = pi." + dateProperty;

        return getQuery( hql )
            .setEntity( "notificationTemplate", template )
            .setString( "activeEnrollmentStatus", ProgramStatus.ACTIVE.name() )
            .setDate( "targetDate", targetDate ).list();
    }

    private String toDateProperty( NotificationTrigger trigger )
    {
        if ( trigger == NotificationTrigger.SCHEDULED_DAYS_ENROLLMENT_DATE )
        {
            return "enrollmentDate";
        }
        else if ( trigger == NotificationTrigger.SCHEDULED_DAYS_INCIDENT_DATE )
        {
            return "incidentDate";
        }

        return null;
    }

    @Override
    protected ProgramInstance postProcessObject( ProgramInstance programInstance )
    {
        return ( programInstance == null || programInstance.isDeleted() ) ? null : programInstance;
    }
}
