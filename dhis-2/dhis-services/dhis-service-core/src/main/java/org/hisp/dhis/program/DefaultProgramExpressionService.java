package org.hisp.dhis.program;

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

import static org.hisp.dhis.program.ProgramExpression.OBJECT_PROGRAM_STAGE_DATAELEMENT;
import static org.hisp.dhis.program.ProgramExpression.OBJECT_PROGRAM_STAGE;
import static org.hisp.dhis.program.ProgramExpression.REPORT_DATE;
import static org.hisp.dhis.program.ProgramExpression.DUE_DATE;
import static org.hisp.dhis.program.ProgramExpression.SEPARATOR_ID;
import static org.hisp.dhis.program.ProgramExpression.SEPARATOR_OBJECT;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chau Thu Tran
 */
@Transactional
public class DefaultProgramExpressionService
    implements ProgramExpressionService
{
    private static final String REGEXP = "\\[(" + OBJECT_PROGRAM_STAGE_DATAELEMENT + "|" + OBJECT_PROGRAM_STAGE + ")"
        + SEPARATOR_OBJECT + "([a-zA-Z0-9\\- ]+[" + SEPARATOR_ID + "([a-zA-Z0-9\\- ]|" + DUE_DATE + "|" + REPORT_DATE
        + ")+]*)\\]";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private GenericStore<ProgramExpression> programExpressionStore;

    public void setProgramExpressionStore( GenericStore<ProgramExpression> programExpressionStore )
    {
        this.programExpressionStore = programExpressionStore;
    }

    private ProgramStageService programStageService;

    public void setProgramStageService( ProgramStageService programStageService )
    {
        this.programStageService = programStageService;
    }

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    // -------------------------------------------------------------------------
    // ProgramExpression CRUD operations
    // -------------------------------------------------------------------------

    @Override
    public int addProgramExpression( ProgramExpression programExpression )
    {
        return programExpressionStore.save( programExpression );
    }

    @Override
    public void updateProgramExpression( ProgramExpression programExpression )
    {
        programExpressionStore.update( programExpression );
    }

    @Override
    public void deleteProgramExpression( ProgramExpression programExpression )
    {
        programExpressionStore.delete( programExpression );
    }

    @Override
    public ProgramExpression getProgramExpression( int id )
    {
        return programExpressionStore.get( id );
    }

    @Override
    public String getProgramExpressionValue( ProgramExpression programExpression,
        ProgramStageInstance programStageInstance, Map<String, String> dataValueMap )
    {
        String value = "";
        
        if ( programExpression.getExpression().contains( ProgramExpression.DUE_DATE ) )
        {
            value = DateUtils.getMediumDateString( programStageInstance.getDueDate() );
        }
        else if ( programExpression.getExpression().contains( ProgramExpression.REPORT_DATE ) )
        {
            value = DateUtils.getMediumDateString( programStageInstance.getExecutionDate() );
        }
        else
        {
            StringBuffer description = new StringBuffer();
            Pattern pattern = Pattern.compile( REGEXP );
            Matcher matcher = pattern.matcher( programExpression.getExpression() );

            while ( matcher.find() )
            {
                String key = matcher.group().replaceAll( "[\\[\\]]", "" ).split( SEPARATOR_OBJECT )[1];

                String dataValue = dataValueMap.get( key );
                
                if ( dataValue == null )
                {
                    return null;
                }

                matcher.appendReplacement( description, dataValue );
            }

            matcher.appendTail( description );

            value = description.toString();
        }

        return value;
    }

    @Override
    public String getExpressionDescription( String programExpression )
    {
        StringBuffer description = new StringBuffer();

        Pattern pattern = Pattern.compile( REGEXP );
        Matcher matcher = pattern.matcher( programExpression );
        int countFormula = 0;
        
        while ( matcher.find() )
        {
            countFormula++;
            
            String match = matcher.group();
            String key = matcher.group(1);
            match = match.replaceAll( "[\\[\\]]", "" );

            String[] info = match.split( SEPARATOR_OBJECT );
            String[] ids = info[1].split( SEPARATOR_ID );

            ProgramStage programStage = programStageService.getProgramStage( ids[0] );
            String name = ids[1];
            
            if ( programStage == null )
            {
                return INVALID_CONDITION;
            }
            else if ( !name.equals( DUE_DATE ) && !name.equals( REPORT_DATE )  )
            {
                DataElement dataElement = dataElementService.getDataElement( name );
                
                if ( dataElement == null )
                {
                    return INVALID_CONDITION;
                }
                else
                {
                    name = dataElement.getDisplayName();
                }
            }

            matcher.appendReplacement( description,
                "[" + key + ProgramExpression.SEPARATOR_OBJECT + programStage.getDisplayName() + SEPARATOR_ID + name + "]" );
        }

        StringBuffer tail = new StringBuffer();
        matcher.appendTail( tail );
        
        if ( countFormula > 1 || !tail.toString().isEmpty() || ( countFormula == 0 && !tail.toString().isEmpty() ) )
        {
            return INVALID_CONDITION;
        }        

        return description.toString();
    }

    @Override
    public Collection<DataElement> getDataElements( String programExpression )
    {
        Collection<DataElement> dataElements = new HashSet<>();

        Pattern pattern = Pattern.compile( REGEXP );
        Matcher matcher = pattern.matcher( programExpression );
        
        while ( matcher.find() )
        {
            String match = matcher.group();
            match = match.replaceAll( "[\\[\\]]", "" );

            String[] info = match.split( SEPARATOR_OBJECT );
            String[] ids = info[1].split( SEPARATOR_ID );
            
            DataElement dataElement = dataElementService.getDataElement( ids[1] );
            dataElements.add( dataElement );
        }

        return dataElements;
    }
}
