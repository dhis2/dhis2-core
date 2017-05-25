package org.hisp.dhis.program;

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

import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Viet Nguyen
 */
@Transactional
public class DefaultProgramTrackedEntityAttributeGroupService
    implements ProgramTrackedEntityAttributeGroupService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private GenericIdentifiableObjectStore<ProgramTrackedEntityAttributeGroup> attributeGroupStore;

    public void setAttributeGroupStore( GenericIdentifiableObjectStore<ProgramTrackedEntityAttributeGroup> attributeGroupStore )
    {
        this.attributeGroupStore = attributeGroupStore;
    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public int addProgramTrackedEntityAttributeGroup( ProgramTrackedEntityAttributeGroup attributeGroup )
    {
        attributeGroupStore.save( attributeGroup );

        return attributeGroup.getId();
    }

    @Override
    public void deleteProgramTrackedEntityAttributeGroup( ProgramTrackedEntityAttributeGroup attributeGroup )
    {
        attributeGroupStore.delete( attributeGroup );
    }

    @Override
    public void updateProgramTrackedEntityAttributeGroup( ProgramTrackedEntityAttributeGroup attributeGroup )
    {
        attributeGroupStore.update( attributeGroup );
    }

    @Override
    public ProgramTrackedEntityAttributeGroup getProgramTrackedEntityAttributeGroup( int id )
    {
        return attributeGroupStore.get( id );
    }

    @Override
    public ProgramTrackedEntityAttributeGroup getProgramTrackedEntityAttributeGroup( String uid )
    {
        return attributeGroupStore.getByUid( uid );
    }

    @Override
    public ProgramTrackedEntityAttributeGroup getProgramTrackedEntityAttributeGroupByName( String name )
    {
        return attributeGroupStore.getByName( name );
    }

    @Override
    public List<ProgramTrackedEntityAttributeGroup> getAllProgramTrackedEntityAttributeGroups()
    {
        return attributeGroupStore.getAll();
    }
}
