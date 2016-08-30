package org.hisp.dhis.relationship;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Abyot Asalefew
 */
@Transactional
public class DefaultRelationshipTypeService
    implements RelationshipTypeService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private RelationshipTypeStore relationshipTypeStore;

    public void setRelationshipTypeStore( RelationshipTypeStore relationshipTypeStore )
    {
        this.relationshipTypeStore = relationshipTypeStore;
    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public void deleteRelationshipType( RelationshipType relationshipType )
    {
        relationshipTypeStore.delete( relationshipType );
    }

    @Override
    public List<RelationshipType> getAllRelationshipTypes()
    {
        return relationshipTypeStore.getAll();
    }

    @Override
    public RelationshipType getRelationshipType( int id )
    {
        return relationshipTypeStore.get( id );
    }

    @Override
    public RelationshipType getRelationshipType( String uid )
    {
        return relationshipTypeStore.getByUid( uid );
    }

    @Override
    public int addRelationshipType( RelationshipType relationshipType )
    {
        return relationshipTypeStore.save( relationshipType );
    }

    @Override
    public void updateRelationshipType( RelationshipType relationshipType )
    {
        relationshipTypeStore.update( relationshipType );
    }

    @Override
    public RelationshipType getRelationshipType( String aIsToB, String bIsToA )
    {
        return relationshipTypeStore.getRelationshipType( aIsToB, bIsToA );
    }

    @Override
    public Integer getRelationshipTypeCountByName( String name )
    {
        return relationshipTypeStore.getCountLikeName( name );
    }

    @Override
    public List<RelationshipType> getRelationshipTypesBetweenByName( String name, int min, int max )
    {
        return relationshipTypeStore.getAllLikeName( name, min, max );
    }

    @Override
    public Integer getRelationshipTypeCount()
    {
        return relationshipTypeStore.getCount();
    }

    @Override
    public List<RelationshipType> getRelationshipTypesBetween( int min, int max )
    {
        return relationshipTypeStore.getAllOrderedName( min, max );
    }
}
