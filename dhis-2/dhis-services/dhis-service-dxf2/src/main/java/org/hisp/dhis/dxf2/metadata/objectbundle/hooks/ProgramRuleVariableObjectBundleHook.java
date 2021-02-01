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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * @author Zubair Asghar.
 */
@Component
public class ProgramRuleVariableObjectBundleHook extends AbstractObjectBundleHook
{
    private final ImmutableMap<ProgramRuleVariableSourceType, Consumer<ProgramRuleVariable>> SOURCE_TYPE_RESOLVER = new ImmutableMap.Builder<ProgramRuleVariableSourceType, Consumer<ProgramRuleVariable>>()
        .put( ProgramRuleVariableSourceType.CALCULATED_VALUE, this::processCalculatedValue )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT, this::processDataElement )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM, this::processDataElement )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_PREVIOUS_EVENT, this::processDataElement )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE, this::processDataElementWithStage )
        .put( ProgramRuleVariableSourceType.TEI_ATTRIBUTE, this::processTEA )
        .build();

    /**
     * Check if a Program Rule Variable to be added or update has a unique name
     * in the system
     *
     * @param object
     * @param bundle
     * @param <T>
     * @return
     */
    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        if ( !(object instanceof ProgramRuleVariable) )
        {
            return super.validate( object, bundle );
        }
        Session session = sessionFactory.getCurrentSession();
        ProgramRuleVariable programRuleVariable = (ProgramRuleVariable) object;

        Query<ProgramRuleVariable> query = session.createQuery(
            " from ProgramRuleVariable prv where prv.name = :name and prv.program.uid = :programUid",
            ProgramRuleVariable.class );

        query.setParameter( "name", programRuleVariable.getName() );
        query.setParameter( "programUid", programRuleVariable.getProgram().getUid() );

        int allowedCount = bundle.getImportMode() == ImportStrategy.UPDATE ? 1 : 0;

        if ( query.getResultList().size() > allowedCount )
        {
            return ImmutableList.of(
                new ErrorReport( ProgramRuleVariable.class, ErrorCode.E4032, programRuleVariable.getName(),
                    programRuleVariable.getProgram().getUid() ) );
        }

        return super.validate( object, bundle );

    }

    @Override
    public <T extends IdentifiableObject> void preUpdate( T object, T persistedObject, ObjectBundle bundle )
    {
        if ( !ProgramRuleVariable.class.isInstance( object ) )
            return;

        ProgramRuleVariable variable = (ProgramRuleVariable) object;

        SOURCE_TYPE_RESOLVER.getOrDefault( variable.getSourceType(), v -> {
        } ).accept( variable );
    }

    private void processCalculatedValue( ProgramRuleVariable variable )
    {
        variable.setAttribute( null );
        variable.setDataElement( null );
        variable.setProgramStage( null );
    }

    private void processDataElement( ProgramRuleVariable variable )
    {
        variable.setAttribute( null );
        variable.setProgramStage( null );
    }

    private void processTEA( ProgramRuleVariable variable )
    {
        variable.setDataElement( null );
        variable.setProgramStage( null );
    }

    private void processDataElementWithStage( ProgramRuleVariable variable )
    {
        variable.setAttribute( null );
    }
}
