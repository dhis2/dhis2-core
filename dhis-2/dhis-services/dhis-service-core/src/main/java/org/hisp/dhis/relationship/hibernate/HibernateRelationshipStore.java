package org.hisp.dhis.relationship.hibernate;

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

import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipStore;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

import java.util.List;

/**
 * @author Abyot Asalefew
 */
public class HibernateRelationshipStore
    extends HibernateIdentifiableObjectStore<Relationship>
    implements RelationshipStore
{
    @Override
    public List<Relationship> getByTrackedEntityInstance( TrackedEntityInstance tei )
    {

        return getCriteria(
            Restrictions.eq( "from.trackedEntityInstance", tei )
        )
            .createAlias( "from", "from" )
            .list();

    }

    @Override
    public List<Relationship> getByProgramInstance( ProgramInstance pi )
    {
        return getCriteria(
            Restrictions.eq( "from.programInstance", pi )
        )
            .createAlias( "from", "from" )
            .list();
    }

    @Override
    public List<Relationship> getByProgramStageInstance( ProgramStageInstance psi )
    {

        return getCriteria(
            Restrictions.eq( "from.programStageInstance", psi )
        )
            .createAlias( "from", "from" )
            .list();
    }
}
