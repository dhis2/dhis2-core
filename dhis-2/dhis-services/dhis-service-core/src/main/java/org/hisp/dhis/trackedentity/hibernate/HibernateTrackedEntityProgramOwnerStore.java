package org.hisp.dhis.trackedentity.hibernate;

import java.util.List;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerStore;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ameen Mohamed
 */
@Transactional
public class HibernateTrackedEntityProgramOwnerStore
    extends HibernateGenericStore<TrackedEntityProgramOwner>
    implements TrackedEntityProgramOwnerStore
{
    private static final Log log = LogFactory.getLog( HibernateTrackedEntityProgramOwnerStore.class );
    
    @Override
    public TrackedEntityProgramOwner getTrackedEntityProgramOwner(int teiId,int programId)
    {
        return (TrackedEntityProgramOwner) getQuery( "from TrackedEntityProgramOwner tepo where tepo.entityInstance.id="+teiId+" and tepo.program.id="+programId ).uniqueResult();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityProgramOwner> getTrackedEntityProgramOwners( List<Integer> teiIds )
    {
        String hql = "from TrackedEntityProgramOwner tepo where tepo.entityInstance.id in (:teiIds)";
        Query q = getQuery(hql);
        q.setParameterList("teiIds", teiIds);
        return q.list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<TrackedEntityProgramOwner> getTrackedEntityProgramOwners( List<Integer> teiIds, int programId )
    {
        String hql = "from TrackedEntityProgramOwner tepo where tepo.entityInstance.id in (:teiIds) and tepo.program.id=(:programId) ";
        Query q = getQuery(hql);
        q.setParameterList("teiIds", teiIds);
        q.setParameter( "programId", programId );
        return q.list();
    }

}
