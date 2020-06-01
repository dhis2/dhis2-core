package org.hisp.dhis.program;

/*
 * Copyright (c) 2004-2018, University of Oslo
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
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.parser.expression.CommonVisitor;
import org.hisp.dhis.parser.expression.InternalParserException;
import org.hisp.dhis.parser.expression.ParserExceptionWithoutContext;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.parser.expression.ParserUtils.*;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.*;

/**
 * Visit the ANTLR parse tree for a program expression, using the ANTLR visitor
 * pattern, for syntax checking and also generating an expression description.
 *
 * @author Markus Bekken
 * @author Jim Grace
 */
public class ProgramValidator
    extends CommonVisitor
{
    private ProgramIndicatorService programIndicatorService;

    private ConstantService constantService;

    private ProgramStageService programStageService;

    private DataElementService dataElementService;

    private TrackedEntityAttributeService attributeService;

    private RelationshipTypeService relationshipTypeService;

    I18n i18n;

    private Map<String, String> itemDescriptions;

    private static final String DEFAULT_DATE_VALUE = "2017-07-08";

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ProgramValidator(
        ProgramIndicatorService programIndicatorService,
        ConstantService constantService,
        ProgramStageService programStageService,
        DataElementService dataElementService,
        TrackedEntityAttributeService attributeService,
        RelationshipTypeService relationshipTypeService,
        I18n i18n,
        Map<String, String> itemDescriptions )
    {
        checkNotNull( programIndicatorService );
        checkNotNull( constantService );
        checkNotNull( programStageService );
        checkNotNull( dataElementService );
        checkNotNull( attributeService );
        checkNotNull( relationshipTypeService );
        checkNotNull( i18n );
        checkNotNull( itemDescriptions );

        this.programIndicatorService = programIndicatorService;
        this.constantService = constantService;
        this.programStageService = programStageService;
        this.dataElementService = dataElementService;
        this.attributeService = attributeService;
        this.relationshipTypeService = relationshipTypeService;
        this.i18n = i18n;
        this.itemDescriptions = itemDescriptions;
    }

    // -------------------------------------------------------------------------
    // Visitor methods
    // -------------------------------------------------------------------------

    @Override
    public Object visitItem( ItemContext ctx )
    {
        switch ( ctx.it.getType() )
        {
            case HASH_BRACE:
                if ( !isStageElementSyntax( ctx ) )
                {
                    throw new ParserExceptionWithoutContext( "Invalid Program Stage / DataElement syntax: " + ctx.getText() );
                }

                ValueType valueType = validateStageDataElement( ctx.getText(), ctx.uid0.getText(), ctx.uid1.getText() );
                return ValidationUtils.getSubstitutionValue( valueType );

            case A_BRACE:
                if ( !isProgramExpressionProgramAttribute( ctx ) )
                {
                    throw new ParserExceptionWithoutContext( "Program attribute must have one UID: " + ctx.getText() );
                }

                valueType = validateProgramAttribute( ctx );
                return ValidationUtils.getSubstitutionValue( valueType );

            case C_BRACE:
                Constant constant = constantService.getConstant( ctx.uid0.getText() );

                if ( constant == null )
                {
                    throw new ParserExceptionWithoutContext( "Constant not found: " + ctx.getText() );
                }

                itemDescriptions.put( ctx.getText(), constant.getDisplayName() );

                return DOUBLE_VALUE_IF_NULL;

            default:
                throw new ParserExceptionWithoutContext( "Item not recognized for program expression: " + ctx.getText() );
        }
    }

    @Override
    public Object visitProgramVariable( ProgramVariableContext ctx )
    {
        String variableName = i18n.getString( ctx.var.getText() );

        itemDescriptions.put( ctx.getText(), variableName );

        switch ( ctx.var.getType() )
        {
            case V_ANALYTICS_PERIOD_END:
            case V_ANALYTICS_PERIOD_START:
            case V_CURRENT_DATE:
            case V_DUE_DATE:
            case V_ENROLLMENT_DATE:
            case V_EVENT_DATE:
            case V_EXECUTION_DATE:
            case V_INCIDENT_DATE:
            case V_COMPLETED_DATE:
                return DEFAULT_DATE_VALUE;

            case V_ENROLLMENT_COUNT:
            case V_EVENT_COUNT:
            case V_ORG_UNIT_COUNT:
            case V_TEI_COUNT:
            case V_VALUE_COUNT:
            case V_ZERO_POS_VALUE_COUNT:
                return 1d;

            case V_ENROLLMENT_STATUS:
                return "COMPLETED";

            case V_PROGRAM_STAGE_ID:
                return "WZbXY0S00lP";

            case V_PROGRAM_STAGE_NAME:
                return "First antenatal care visit";

            default:
                throw new InternalParserException( "Program variable " + ctx.var.getText() + " not recognized." );
        }
    }

    @Override
    public Object visitProgramFunction( ProgramFunctionContext ctx )
    {
        validateProgramFunctionDateArgs( ctx );

        ValueType dataElementValueType = validateStageDataElement( ctx.stageDataElement() );

        switch ( ctx.d2.getType() )
        {
            case D2_CONDITION:
                return validateCondition( ctx );

            case D2_COUNT:
            case D2_DAYS_BETWEEN:
            case D2_MINUTES_BETWEEN:
            case D2_MONTHS_BETWEEN:
            case D2_WEEKS_BETWEEN:
            case D2_YEARS_BETWEEN:
                return 1d;

            case D2_COUNT_IF_VALUE:
                validateCountIfValue( ctx, dataElementValueType );
                return 1d;

            case D2_COUNT_IF_CONDITION:
                validateCountIfCondition( ctx, dataElementValueType );
                return 1d;

            case D2_HAS_VALUE:
                visit( ctx.expr( 0 ) );
                return true;

            case D2_OIZP:
            case D2_ZING:
            case AVG:
            case COUNT:
            case MAX:
            case MIN:
            case STDDEV:
            case SUM:
            case VARIANCE:
                castDouble( visit( ctx.expr( 0 ) ) );
                return 1d;

            case D2_ZPVC:
                ctx.item().stream().forEach( i -> castDouble( visit( i ) ) );

            case D2_RELATIONSHIP_COUNT:
                validateRelationshipType( ctx );
                return 1d;

            default:
                throw new InternalParserException("Program function " + ctx.d2.getText() + " not recognized." );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void validateProgramFunctionDateArgs( ProgramFunctionContext ctx )
    {
        if ( ctx.compareDate().size() != 0 )
        {
            Assert.isTrue( ctx.compareDate().size() == 2, "validateDateArgs found " + ctx.compareDate().size() + " compare dates" );

            validateDateArg( ctx.compareDate( 0 ) );
            validateDateArg( ctx.compareDate( 1 ) );
        }
    }

    private void validateDateArg( CompareDateContext ctx )
    {
        if ( ctx.uid0 != null )
        {
            String programStageUid = ctx.uid0.getText();

            ProgramStage programStage = programStageService.getProgramStage( programStageUid );

            if ( programStage == null )
            {
                throw new ParserExceptionWithoutContext( "Program stage " + ctx.uid0.getText() + " not found" );
            }

            itemDescriptions.put( programStageUid, programStage.getDisplayName() );

            return;
        }

        castDate( visit( ctx.expr() ) );
    }

    private ValueType validateStageDataElement( StageDataElementContext ctx )
    {
        return ctx == null ? null : validateStageDataElement( ctx.getText(), ctx.uid0.getText(), ctx.uid1.getText() );
    }

    private ValueType validateStageDataElement( String text, String programStageId, String dataElementId )
    {
        ProgramStage programStage = programStageService.getProgramStage( programStageId );
        DataElement dataElement = dataElementService.getDataElement( dataElementId );

        if ( programStage == null )
        {
            throw new ParserExceptionWithoutContext( "Program stage " + programStageId + " not found" );
        }

        if ( dataElement == null )
        {
            throw new ParserExceptionWithoutContext( "Data element " + dataElementId + " not found" );
        }

        String description = programStage.getDisplayName() + ProgramIndicator.SEPARATOR_ID + dataElement.getDisplayName();

        itemDescriptions.put( text, description );

        return dataElement.getValueType();
    }

    private ValueType validateProgramAttribute( ItemContext ctx )
    {
        return validateProgramAttribute( ctx.getText(), ctx.uid0.getText() );
    }

    private ValueType validateProgramAttribute( ProgramAttributeContext ctx )
    {
        return validateProgramAttribute( ctx.getText(), ctx.uid0.getText() );
    }

    private ValueType validateProgramAttribute( String text, String attributeId )
    {
        TrackedEntityAttribute attribute = attributeService.getTrackedEntityAttribute( attributeId );

        if ( attribute == null )
        {
            throw new ParserExceptionWithoutContext( "Tracked entity attribute " + attributeId + " not found." );
        }

        itemDescriptions.put( text, attribute.getDisplayName() );

        return attribute.getValueType();
    }

    private Object validateCondition( ProgramFunctionContext ctx )
    {
        validateSubexprssion( trimQuotes( ctx.stringLiteral().getText() ), Boolean.class );

        Object valueIfTrue = visit( ctx.expr( 0 ) );
        Object valueIfFalse = visit( ctx.expr( 1 ) );

        castClass( valueIfTrue.getClass(), valueIfFalse );

        return valueIfTrue;
    }

    private void validateCountIfValue( ProgramFunctionContext ctx, ValueType dataElementValueType )
    {
        String dataValueCheck = ValidationUtils.dataValueIsValid( ctx.numStringLiteral().getText(), dataElementValueType );

        if ( dataValueCheck != null )
        {
            throw new ParserExceptionWithoutContext( dataValueCheck );
        }
    }

    private void validateCountIfCondition( ProgramFunctionContext ctx, ValueType dataElementValueType )
    {
        String testExpression = ValidationUtils.getSubstitutionValue( dataElementValueType ) + trimQuotes( ctx.stringLiteral().getText() );

        validateSubexprssion( testExpression, Boolean.class );
    }

    private void validateRelationshipType( ProgramFunctionContext ctx )
    {
        if ( ctx.QUOTED_UID() != null )
        {
            String relationshipId = trimQuotes( ctx.QUOTED_UID().getText() );

            RelationshipType relationshipType = relationshipTypeService.getRelationshipType( relationshipId );

            if ( relationshipType == null )
            {
                throw new ParserExceptionWithoutContext( "Relationship type " + relationshipId + " not found" );
            }

            itemDescriptions.put( relationshipId, relationshipType.getDisplayName() );
        }
    }

    private void validateSubexprssion( String subExpression, Class<?> clazz )
    {
        programIndicatorService.validate( subExpression, clazz, itemDescriptions );
    }
}
