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

import org.apache.commons.math3.util.MathUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hisp.dhis.program.ProgramExpression.*;

/**
 * @author Chau Thu Tran
 */
@Transactional
public class DefaultProgramValidationService
    implements ProgramValidationService
{
    private final String regExp = "\\[(" + OBJECT_PROGRAM_STAGE_DATAELEMENT + "|" + OBJECT_PROGRAM_STAGE + ")"
        + SEPARATOR_OBJECT + "([a-zA-Z0-9\\- ]+[" + SEPARATOR_ID + "[a-zA-Z0-9\\- ]*]*)" + "\\]";

    private ProgramValidationStore validationStore;

    public void setValidationStore( ProgramValidationStore validationStore )
    {
        this.validationStore = validationStore;
    }

    private ProgramExpressionService expressionService;

    public void setExpressionService( ProgramExpressionService expressionService )
    {
        this.expressionService = expressionService;
    }

    private TrackedEntityDataValueService dataValueService;

    public void setDataValueService( TrackedEntityDataValueService dataValueService )
    {
        this.dataValueService = dataValueService;
    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------
    @Override
    public int addProgramValidation( ProgramValidation programValidation )
    {
        return validationStore.save( programValidation );
    }

    @Override
    public void updateProgramValidation( ProgramValidation programValidation )
    {
        validationStore.update( programValidation );
    }

    @Override
    public void deleteProgramValidation( ProgramValidation programValidation )
    {
        validationStore.delete( programValidation );
    }

    @Override
    public List<ProgramValidation> getAllProgramValidation()
    {
        return validationStore.getAll();
    }

    @Override
    public ProgramValidation getProgramValidation( int id )
    {
        return validationStore.get( id );
    }

    @Override
    public List<ProgramValidationResult> validate( Collection<ProgramValidation> validation,
        ProgramStageInstance programStageInstance )
    {
        List<ProgramValidationResult> result = new ArrayList<>();

        // ---------------------------------------------------------------------
        // Get data-values
        // ---------------------------------------------------------------------

        Program program = programStageInstance.getProgramInstance().getProgram();
        Collection<TrackedEntityDataValue> entityInstanceDataValues = null;
        if ( program.isWithoutRegistration() )
        {
            entityInstanceDataValues = dataValueService.getTrackedEntityDataValues( programStageInstance );
        }
        else
        {
            entityInstanceDataValues = dataValueService.getTrackedEntityDataValues( programStageInstance.getProgramInstance()
                .getProgramStageInstances() );
        }

        Map<String, String> entityInstanceDataValueMap = new HashMap<>( entityInstanceDataValues.size() );
        for ( TrackedEntityDataValue entityInstanceDataValue : entityInstanceDataValues )
        {
            String key = entityInstanceDataValue.getProgramStageInstance().getProgramStage().getUid() + "."
                + entityInstanceDataValue.getDataElement().getUid();
            entityInstanceDataValueMap.put( key, entityInstanceDataValue.getValue() );
        }

        // ---------------------------------------------------------------------
        // Validate rules
        // ---------------------------------------------------------------------

        for ( ProgramValidation validate : validation )
        {
            String leftSideValue = expressionService.getProgramExpressionValue( validate.getLeftSide(),
                programStageInstance, entityInstanceDataValueMap );
            String rightSideValue = expressionService.getProgramExpressionValue( validate.getRightSide(),
                programStageInstance, entityInstanceDataValueMap );
            String operator = validate.getOperator().getMathematicalOperator();

            if ( leftSideValue != null && rightSideValue != null )
            {
                String expression = validate.getLeftSide().getExpression() + " " + validate.getRightSide().getExpression();
                if ( isNumberDataExpression( expression ) )
                {
                    double leftSide = Double.parseDouble( leftSideValue );
                    double rightSide = Double.parseDouble( rightSideValue );
                    if ( !((operator.equals( "==" ) && MathUtils.equals( leftSide, rightSide ) )
                        || (operator.equals( "<" ) && leftSide < rightSide)
                        || (operator.equals( "<=" ) && leftSide <= rightSide)
                        || (operator.equals( ">" ) && leftSide > rightSide)
                        || (operator.equals( ">=" ) && leftSide >= rightSide) || (operator.equals( "!=" ) && !MathUtils.equals( leftSide, rightSide ) )) )
                    {
                        ProgramValidationResult validationResult = new ProgramValidationResult( programStageInstance,
                            validate, leftSideValue, rightSideValue );
                        result.add( validationResult );
                    }
                }
                else if ( !((operator.equals( "==" ) && leftSideValue.compareTo( rightSideValue ) == 0)
                    || (operator.equals( "<" ) && leftSideValue.compareTo( rightSideValue ) < 0)
                    || (operator.equals( "<=" ) && (leftSideValue.compareTo( rightSideValue ) <= 0))
                    || (operator.equals( ">" ) && leftSideValue.compareTo( rightSideValue ) > 0)
                    || (operator.equals( ">=" ) && leftSideValue.compareTo( rightSideValue ) >= 0) || (operator
                    .equals( "!=" ) && leftSideValue.compareTo( rightSideValue ) == 0)) )
                {
                    ProgramValidationResult validationResult = new ProgramValidationResult( programStageInstance,
                        validate, leftSideValue, rightSideValue );
                    result.add( validationResult );
                }
            }
        }

        return result;
    }

    @Override
    public List<ProgramValidation> getProgramValidation( Program program )
    {
        return validationStore.get( program );
    }

    @Override
    public List<ProgramValidation> getProgramValidation( ProgramStage programStage )
    {
        List<ProgramValidation> programValidation = getProgramValidation( programStage.getProgram() );

        Iterator<ProgramValidation> iter = programValidation.iterator();

        Pattern pattern = Pattern.compile( regExp );

        while ( iter.hasNext() )
        {
            ProgramValidation validation = iter.next();

            String expression = validation.getLeftSide().getExpression() + " "
                + validation.getRightSide().getExpression();
            Matcher matcher = pattern.matcher( expression );

            boolean flag = false;
            while ( matcher.find() )
            {
                String match = matcher.group();
                match = match.replaceAll( "[\\[\\]]", "" );

                String[] info = match.split( SEPARATOR_OBJECT );
                String[] ids = info[1].split( SEPARATOR_ID );

                String programStageUid = ids[0];

                if ( programStageUid.equals( programStage.getUid() ) )
                {
                    flag = true;
                    break;
                }
            }

            if ( !flag )
            {
                iter.remove();
            }
        }

        return programValidation;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private boolean isNumberDataExpression( String programExpression )
    {
        Collection<DataElement> dataElements = expressionService.getDataElements( programExpression );

        for ( DataElement dataElement : dataElements )
        {
            if ( dataElement.getValueType().isNumeric() )
            {
                return true;
            }
        }

        return false;
    }
}
