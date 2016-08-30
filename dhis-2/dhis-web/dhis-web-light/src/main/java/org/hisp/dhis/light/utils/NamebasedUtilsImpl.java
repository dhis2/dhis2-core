package org.hisp.dhis.light.utils;

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

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.mobile.service.ModelMapping;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.system.util.MathUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class NamebasedUtilsImpl
    implements NamebasedUtils
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramService programService;

    public void setProgramService( ProgramService programService )
    {
        this.programService = programService;
    }

    private ProgramStageService programStageService;

    public void setProgramStageService( ProgramStageService programStageService )
    {
        this.programStageService = programStageService;
    }

    @Override
    public ProgramStage getProgramStage( int programId, int programStageId )
    {
        Program program = programService.getProgram( programId );

        Collection<ProgramStage> stages = program.getProgramStages();

        for ( ProgramStage programStage : stages )
        {
            if ( programStage.getId() == programStageId )
            {
                return programStage;
            }
        }
        return null;
    }

    @Override
    public String getTypeViolation( DataElement dataElement, String value )
    {
        ValueType valueType = dataElement.getValueType();

        if ( valueType.isText() )
        {
        }
        else if ( ValueType.BOOLEAN == valueType )
        {
            if ( !ValueUtils.isBoolean( value ) )
            {
                return "is_invalid_boolean";
            }
        }
        else if ( ValueType.DATE == valueType )
        {
            if ( !ValueUtils.isDate( value ) )
            {
                return "is_invalid_date";
            }
        }
        else if ( ValueType.NUMBER == valueType )
        {
            if ( !MathUtils.isNumeric( value ) )
            {
                return "is_invalid_number";
            }
        }
        else if ( ValueType.INTEGER == valueType )
        {
            if ( !MathUtils.isInteger( value ) )
            {
                return "is_invalid_integer";
            }
        }
        else if ( ValueType.INTEGER_POSITIVE == valueType )
        {
            if ( !MathUtils.isPositiveInteger( value ) )
            {
                return "is_invalid_positive_integer";
            }
        }
        else if ( ValueType.INTEGER_NEGATIVE == valueType )
        {
            if ( !MathUtils.isNegativeInteger( value ) )
            {
                return "is_invalid_negative_integer";
            }
        }
        else if ( ValueType.INTEGER_ZERO_OR_POSITIVE == valueType )
        {
            if ( !MathUtils.isZeroOrPositiveInteger( value ) )
            {
                return "is_invalid_zero_or_positive_integer";
            }
        }
        else if ( ValueType.COORDINATE == valueType )
        {
            if ( !MathUtils.isCoordinate( value ) )
            {
                return "is_invalid_coordinate";
            }
        }
        return null;
    }

    @Override
    public ProgramStageInstance getNextStage( Set<ProgramStageInstance> programStageInstances )
    {
        for ( ProgramStageInstance programStageInstance : programStageInstances )
        {
            if ( !programStageInstance.isCompleted() )
            {
                return programStageInstance;
            }
        }

        return null;
    }

    @Override
    public List<org.hisp.dhis.api.mobile.model.DataElement> transformDataElementsToMobileModel( Integer programStageId )
    {
        ProgramStage programStage = programStageService.getProgramStage( programStageId );

        List<org.hisp.dhis.api.mobile.model.DataElement> des = new ArrayList<>();

        List<ProgramStageDataElement> programStageDataElements = new ArrayList<>( programStage.getProgramStageDataElements() );

        des = transformDataElementsToMobileModel( programStageDataElements );

        return des;
    }

    @Override
    public List<org.hisp.dhis.api.mobile.model.DataElement> transformDataElementsToMobileModel( List<ProgramStageDataElement> programStageDataElements )
    {
        List<org.hisp.dhis.api.mobile.model.DataElement> des = new ArrayList<>();

        for ( ProgramStageDataElement programStagedataElement : programStageDataElements )
        {
            DataElement dataElement = programStagedataElement.getDataElement();

            org.hisp.dhis.api.mobile.model.DataElement de = ModelMapping.getDataElement( dataElement );

            de.setCompulsory( programStagedataElement.isCompulsory() );

            des.add( de );
        }
        return des;
    }
}
