/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.deduplication;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service( "org.hisp.dhis.deduplication.DeduplicationService" )
public class DefaultDeduplicationService
    implements DeduplicationService
{

    private final PotentialDuplicateStore potentialDuplicateStore;

    public DefaultDeduplicationService( PotentialDuplicateStore potentialDuplicateStore )
    {
        this.potentialDuplicateStore = potentialDuplicateStore;
    }

    @Override
    @Transactional
    public long addPotentialDuplicate( PotentialDuplicate potentialDuplicate )
    {
        potentialDuplicateStore.save( potentialDuplicate );
        return potentialDuplicate.getId();
    }

    @Override
    @Transactional( readOnly = true )
    public PotentialDuplicate getPotentialDuplicateById( long id )
    {
        return potentialDuplicateStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public PotentialDuplicate getPotentialDuplicateByUid( String uid )
    {
        return potentialDuplicateStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<PotentialDuplicate> getAllPotentialDuplicates()
    {
        return potentialDuplicateStore.getAll();
    }

    @Override
    @Transactional
    public void markPotentialDuplicateInvalid( PotentialDuplicate potentialDuplicate )
    {
        potentialDuplicate.setStatus( DeduplicationStatus.INVALID );
        potentialDuplicateStore.update( potentialDuplicate );
    }

    @Override
    @Transactional( readOnly = true )
    public int countPotentialDuplicates( PotentialDuplicateQuery query )
    {

        return potentialDuplicateStore.getCountByQuery( query );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean exists( PotentialDuplicate potentialDuplicate )
    {
        return potentialDuplicateStore.exists( potentialDuplicate );
    }

    @Override
    @Transactional( readOnly = true )
    public List<PotentialDuplicate> getAllPotentialDuplicates( PotentialDuplicateQuery query )
    {
        return potentialDuplicateStore.getAllByQuery( query );
    }

    @Override
    @Transactional
    public void deletePotentialDuplicate( PotentialDuplicate potentialDuplicate )
    {
        potentialDuplicateStore.delete( potentialDuplicate );
    }
}
