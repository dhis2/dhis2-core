package org.hisp.dhis.analytics.hibernate;

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

import java.util.List;

import org.hisp.dhis.analytics.AnalyticsTablePhase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.AnalyticsTableHook;
import org.hisp.dhis.analytics.AnalyticsTableHookStore;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.resourcetable.ResourceTableType;

/**
 * @author Lars Helge Overland
 */
public class HibernateAnalyticsTableHookStore
    extends HibernateIdentifiableObjectStore<AnalyticsTableHook>
    implements AnalyticsTableHookStore
{
    private static final Log log = LogFactory.getLog( HibernateAnalyticsTableHookStore.class );
    
    @Override
    @SuppressWarnings("unchecked")
    public List<AnalyticsTableHook> getByPhase( AnalyticsTablePhase phase )
    {
        return getJpaQuery( "from AnalyticsTableHook h where h.phase = :phase" )
            .setParameter( "phase", phase )
            .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AnalyticsTableHook> getByPhaseAndResourceTableType( AnalyticsTablePhase phase, ResourceTableType resourceTableType )
    {
        return getJpaQuery( "from AnalyticsTableHook h where h.phase = :phase and h.resourceTableType = :resourceTableType" )
            .setParameter( "phase", phase )
            .setParameter( "resourceTableType", resourceTableType )
            .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AnalyticsTableHook> getByPhaseAndAnalyticsTableType( AnalyticsTablePhase phase, AnalyticsTableType analyticsTableType )
    {
        return getJpaQuery( "from AnalyticsTableHook h where h.phase = :phase and h.analyticsTableType = :analyticsTableType" )
            .setParameter( "phase", phase )
            .setParameter( "analyticsTableType", analyticsTableType )
            .getResultList();
    }

    @Override
    public void executeAnalyticsTableSqlHooks( List<AnalyticsTableHook> hooks )
    {
        for ( AnalyticsTableHook hook : hooks )
        {
            log.info( String.format( "Executing analytics table hook: '%s', '%s'", hook.getUid(), hook.getName() ) );
            
            jdbcTemplate.execute( hook.getSql() );
        }
    }
}
