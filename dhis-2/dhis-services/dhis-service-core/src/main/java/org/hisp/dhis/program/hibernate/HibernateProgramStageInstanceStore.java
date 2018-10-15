package org.hisp.dhis.program.hibernate;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.time.DateUtils;
import org.hibernate.query.Query;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

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
    public ProgramStageInstance get( ProgramInstance programInstance, ProgramStage programStage )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        List<ProgramStageInstance> list = getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "programInstance" ), programInstance ) )
            .addPredicate( root -> builder.equal( root.get( "programStage" ), programStage ) )
            .addOrder( root -> builder.asc( root.get( "id" ) ) )
            .setMaxResults( 1 ) );

        return list.isEmpty() ? null : list.get( 0 );
    }

    @Override
    public List<ProgramStageInstance> get( Collection<ProgramInstance> programInstances, EventStatus status )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "status" ), status ) )
            .addPredicate( root -> root.get( "programInstance" ).in( programInstances ) ) );
    }

    @Override
    public List<ProgramStageInstance> get( TrackedEntityInstance entityInstance, EventStatus status )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "status" ), status ) )
            .addPredicate( root -> builder.equal( root.join( "programInstance" ).get( "entityInstance" ), entityInstance ) ) );
    }

    @Override
    public long getProgramStageInstanceCountLastUpdatedAfter( Date time )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getCount( builder, newJpaParameters()
            .addPredicate( root -> builder.greaterThanOrEqualTo( root.get( "lastUpdated" ), time ) )
            .count( root -> builder.countDistinct( root ) ) );
    }

    @Override
    public boolean exists( String uid )
    {
        Integer result = jdbcTemplate.queryForObject( "select count(*) from programstageinstance where uid=? and deleted is false", Integer.class, uid );
        return result != null && result > 0;
    }

    @Override
    public boolean existsIncludingDeleted( String uid )
    {
        Integer result = jdbcTemplate.queryForObject( "select count(*) from programstageinstance where uid=?", Integer.class, uid );
        return result != null && result > 0;
    }

    @Override
    public void updateProgramStageInstancesSyncTimestamp( List<String> programStageInstanceUIDs, Date lastSynchronized )
    {
        String hql = "update ProgramStageInstance set lastSynchronized = :lastSynchronized WHERE uid in :programStageInstances";
        Query query = getQuery( hql );
        query.setParameter( "lastSynchronized", lastSynchronized );
        query.setParameter( "programStageInstances", programStageInstanceUIDs );

        query.executeUpdate();
    }

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
            .setParameter( "notificationTemplate", template )
            .setParameter( "skippedEventStatus", EventStatus.SKIPPED )
            .setParameter( "targetDate", targetDate ).list();
    }

    @Override
    protected void preProcessPredicates( CriteriaBuilder builder, List<Function<Root<ProgramStageInstance>, Predicate>> predicates )
    {
        predicates.add( root -> builder.equal( root.get( "deleted" ), false ) );
    }

    @Override
    protected ProgramStageInstance postProcessObject( ProgramStageInstance programStageInstance )
    {
        return (programStageInstance == null || programStageInstance.isDeleted()) ? null : programStageInstance;
    }
}
