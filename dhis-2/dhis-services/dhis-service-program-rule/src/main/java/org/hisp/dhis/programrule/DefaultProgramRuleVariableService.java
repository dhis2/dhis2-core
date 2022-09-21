/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.programrule;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.commons.util.SystemUtils.isTestRun;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author markusbekken
 */
@Service( "org.hisp.dhis.programrule.ProgramRuleVariableService" )
public class DefaultProgramRuleVariableService
    implements ProgramRuleVariableService
{

    private final Environment environment;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramRuleVariableStore programRuleVariableStore;

    private final Cache<Boolean> programRuleVariablesCache;

    public DefaultProgramRuleVariableService( ProgramRuleVariableStore programRuleVariableStore,
        CacheProvider cacheProvider, Environment environment )
    {
        checkNotNull( programRuleVariableStore );
        checkNotNull( environment );

        this.programRuleVariableStore = programRuleVariableStore;
        this.environment = environment;
        final boolean nonTestEnv = !isTestRun( this.environment.getActiveProfiles() );

        this.programRuleVariablesCache = cacheProvider.newCacheBuilder( Boolean.class )
            .forRegion( "programRuleVariablesCache" )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withMaximumSize( nonTestEnv ? 1000 : 0 )
            .build();
    }

    // -------------------------------------------------------------------------
    // ProgramRuleVariable implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addProgramRuleVariable( ProgramRuleVariable programRuleVariable )
    {
        programRuleVariableStore.save( programRuleVariable );
        return programRuleVariable.getId();
    }

    @Override
    @Transactional
    public void deleteProgramRuleVariable( ProgramRuleVariable programRuleVariable )
    {
        programRuleVariableStore.delete( programRuleVariable );
        programRuleVariablesCache.invalidateAll();
    }

    @Override
    @Transactional
    public void updateProgramRuleVariable( ProgramRuleVariable programRuleVariable )
    {
        programRuleVariableStore.update( programRuleVariable );
    }

    @Override
    @Transactional( readOnly = true )
    public ProgramRuleVariable getProgramRuleVariable( long id )
    {
        return programRuleVariableStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramRuleVariable> getAllProgramRuleVariable()
    {
        return programRuleVariableStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramRuleVariable> getProgramRuleVariable( Program program )
    {
        return programRuleVariableStore.get( program );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean isLinkedToProgramRuleVariableCached( Program program, DataElement dataElement )
    {
        return programRuleVariablesCache.get( dataElement.getUid(), uid -> {
            List<ProgramRuleVariable> ruleVariables = programRuleVariableStore
                .getProgramVariables( program, dataElement );
            return !ruleVariables.isEmpty();
        } )
            .orElse( false );
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramRuleVariable> getVariablesWithNoDataElement()
    {
        return programRuleVariableStore.getVariablesWithNoDataElement();
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramRuleVariable> getVariablesWithNoAttribute()
    {
        return programRuleVariableStore.getVariablesWithNoAttribute();
    }
}
