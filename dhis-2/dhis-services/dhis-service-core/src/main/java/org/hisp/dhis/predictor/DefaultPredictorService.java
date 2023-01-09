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
package org.hisp.dhis.predictor;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.IdentifiableObjectStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ken Haase
 * @author Jim Grace
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.predictor.PredictorService" )
public class DefaultPredictorService
    implements PredictorService
{
    private final PredictorStore predictorStore;

    @Qualifier( "org.hisp.dhis.predictor.PredictorGroupStore" )
    private final IdentifiableObjectStore<PredictorGroup> predictorGroupStore;

    // -------------------------------------------------------------------------
    // Predictor
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addPredictor( Predictor predictor )
    {
        predictorStore.save( predictor );
        return predictor.getId();
    }

    @Override
    @Transactional
    public void updatePredictor( Predictor predictor )
    {
        predictorStore.update( predictor );
    }

    @Override
    @Transactional
    public void deletePredictor( Predictor predictor )
    {
        predictorStore.delete( predictor );
    }

    @Override
    @Transactional( readOnly = true )
    public Predictor getPredictor( long id )
    {
        return predictorStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public Predictor getPredictor( String uid )
    {
        return predictorStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Predictor> getAllPredictors()
    {
        return predictorStore.getAll();
    }

    // -------------------------------------------------------------------------
    // Predictor group
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addPredictorGroup( PredictorGroup predictorGroup )
    {
        predictorGroupStore.save( predictorGroup );

        return predictorGroup.getId();
    }

    @Override
    @Transactional
    public void deletePredictorGroup( PredictorGroup predictorGroup )
    {
        predictorGroupStore.delete( predictorGroup );
    }

    @Override
    @Transactional
    public void updatePredictorGroup( PredictorGroup predictorGroup )
    {
        predictorGroupStore.update( predictorGroup );
    }

    @Override
    @Transactional( readOnly = true )
    public PredictorGroup getPredictorGroup( long id )
    {
        return predictorGroupStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public PredictorGroup getPredictorGroup( String uid )
    {
        return predictorGroupStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<PredictorGroup> getAllPredictorGroups()
    {
        return predictorGroupStore.getAll();
    }
}
