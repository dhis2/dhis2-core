package org.hisp.dhis.trackedentity.hibernate;
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

import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceAudit;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceAuditQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceAuditStore;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.system.util.DateUtils.getMediumDateString;

import java.util.List;

/**
 * @author Abyot Asalefew Gizaw abyota@gmail.com
 *
 */
public class HibernateTrackedEntityInstanceAuditStore
    extends HibernateGenericStore<TrackedEntityInstanceAudit>
    implements TrackedEntityInstanceAuditStore
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    
    // -------------------------------------------------------------------------
    // TrackedEntityInstanceAuditService implementation
    // -------------------------------------------------------------------------

    @Override
    public void deleteTrackedEntityInstanceAudit( TrackedEntityInstance trackedEntityInstance )
    {
        String hql = "delete TrackedEntityInstanceAudit where trackedEntityInstance = :trackedEntityInstance";
        getSession().createQuery( hql ).setParameter( "trackedEntityInstance", trackedEntityInstance ).executeUpdate();        
    }

    @Override
    @SuppressWarnings( "unchecked" )    
    public List<TrackedEntityInstanceAudit> getTrackedEntityInstanceAudits( TrackedEntityInstanceAuditQueryParams params )
    {
        SqlHelper hlp = new SqlHelper();

        String hql = "select t from TrackedEntityInstanceAudit t ";
        
        if( params.hasTrackedEntityInstances() )
        {
            hql += hlp.whereAnd() + " t.trackedEntityInstance.uid in (" + getQuotedCommaDelimitedString( getUids( params.getTrackedEntityInstances() ) ) + ") ";
        }
        
        if ( params.hasStartDate() )
        {
            hql += hlp.whereAnd() + " t.created >= '" + getMediumDateString( params.getStartDate() ) + "' ";
        }

        if ( params.hasEndDate() )
        {
            hql += hlp.whereAnd() + " t.created <= '" + getMediumDateString( params.getEndDate() ) + "' ";
        }
        
        hql += "order by t.trackedEntityInstance, t.created";
        
        return getQuery( hql ).list();
    }

}
