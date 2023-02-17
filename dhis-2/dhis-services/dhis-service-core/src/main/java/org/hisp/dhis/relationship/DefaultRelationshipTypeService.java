/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.relationship;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.relationship.RelationshipTypeService" )
public class DefaultRelationshipTypeService
    implements RelationshipTypeService
{
    private final RelationshipTypeStore relationshipTypeStore;

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteRelationshipType( RelationshipType relationshipType )
    {
        relationshipTypeStore.delete( relationshipType );
    }

    @Override
    @Transactional( readOnly = true )
    public List<RelationshipType> getAllRelationshipTypes()
    {
        return relationshipTypeStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public RelationshipType getRelationshipType( long id )
    {
        return relationshipTypeStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public RelationshipType getRelationshipType( String uid )
    {
        return relationshipTypeStore.getByUid( uid );
    }

    @Override
    @Transactional
    public long addRelationshipType( RelationshipType relationshipType )
    {
        relationshipTypeStore.save( relationshipType );
        return relationshipType.getId();
    }

    @Override
    @Transactional
    public void updateRelationshipType( RelationshipType relationshipType )
    {
        relationshipTypeStore.update( relationshipType );
    }

    @Override
    @Transactional( readOnly = true )
    public RelationshipType getRelationshipType( String aIsToB, String bIsToA )
    {
        return relationshipTypeStore.getRelationshipType( aIsToB, bIsToA );
    }
}
