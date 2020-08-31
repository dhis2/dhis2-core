package org.hisp.dhis.programrule.engine;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.rules.models.RuleValidationResult;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.junit.Assert.*;

/**
 * @author Zubair Asghar
 */
public class ProgramRuleEngineDescriptionTest extends DhisSpringTest
{
    private String conditionTextAtt = "A{Program_Rule_Variable_Text_Attr} == 'text_att' || d2:hasValue(V{current_date})";
    private String conditionNumericAtt = "A{Program_Rule_Variable_Numeric_Attr} == 12 || d2:hasValue(V{current_date})";
    private String conditionNumericAttWithOR = "A{Program_Rule_Variable_Numeric_Attr} == 12 or d2:hasValue(V{current_date})";
    private String conditionNumericAttWithAND = "A{Program_Rule_Variable_Numeric_Attr} == 12 and d2:hasValue(V{current_date})";
    private String conditionTextDE = "#{Program_Rule_Variable_Text_DE} == 'text_de'";
    private String incorrectConditionTextDE = "#{Program_Rule_Variable_Text_DE} == 'text_de' +";
    private String conditionNumericDE = "#{Program_Rule_Variable_Numeric_DE} == 14";
    private String conditionLiteralString = "1 > 2 ";

    private DataElement textDataElement;
    private DataElement numericDataElement;

    private TrackedEntityAttribute textAttribute;
    private TrackedEntityAttribute numericAttribute;

    private Constant constantPI;
    private Constant constantArea;

    private Program program;

    private ProgramRule programRuleTextAtt;
    private ProgramRule programRuleNumericAtt;
    private ProgramRule programRuleTextDE;
    private ProgramRule programRuleNumericDE;

    private ProgramRuleVariable programRuleVariableTextDE;
    private ProgramRuleVariable programRuleVariableTextAtt;
    private ProgramRuleVariable programRuleVariableNumericDE;
    private ProgramRuleVariable programRuleVariableNumericAtt;

    @Qualifier( "serviceTrackerRuleEngine" )
    @Autowired
    private ProgramRuleEngine programRuleEngineNew;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private ConstantService constantService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramRuleVariableService ruleVariableService;

    @Autowired
    private ProgramRuleService programRuleService;

    @Before
    public void setUp()
    {
        constantPI = createConstant( 'P', 3.14 );
        constantArea = createConstant( 'A', 22.1 );

        textDataElement = createDataElement( 'D' );
        textDataElement.setValueType( ValueType.TEXT );
        numericDataElement = createDataElement( 'E' );
        numericDataElement.setValueType( ValueType.NUMBER );

        textAttribute = createTrackedEntityAttribute( 'A' );
        textAttribute.setValueType( ValueType.TEXT );
        numericAttribute = createTrackedEntityAttribute( 'B' );
        numericAttribute.setValueType( ValueType.NUMBER );

        constantService.saveConstant( constantPI );
        constantService.saveConstant( constantArea );
        dataElementService.addDataElement( textDataElement );
        dataElementService.addDataElement( numericDataElement );
        attributeService.addTrackedEntityAttribute( textAttribute );
        attributeService.addTrackedEntityAttribute( numericAttribute );

        program = createProgram( 'P' );
        programService.addProgram( program );

        programRuleVariableTextAtt = createProgramRuleVariable( 'R', program );
        programRuleVariableNumericAtt = createProgramRuleVariable( 'S', program );
        programRuleVariableTextDE = createProgramRuleVariable( 'T', program );
        programRuleVariableNumericDE = createProgramRuleVariable( 'U', program );

        programRuleVariableTextAtt.setName( "Program_Rule_Variable_Text_Attr" );
        programRuleVariableTextAtt.setAttribute( textAttribute );
        programRuleVariableTextAtt.setSourceType( ProgramRuleVariableSourceType.TEI_ATTRIBUTE );
        programRuleVariableNumericAtt.setName( "Program_Rule_Variable_Numeric_Attr" );
        programRuleVariableNumericAtt.setSourceType( ProgramRuleVariableSourceType.TEI_ATTRIBUTE );
        programRuleVariableNumericAtt.setAttribute( numericAttribute );

        programRuleVariableTextDE.setName( "Program_Rule_Variable_Text_DE" );
        programRuleVariableTextDE.setDataElement( textDataElement );
        programRuleVariableTextDE.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );
        programRuleVariableNumericDE.setName( "Program_Rule_Variable_Numeric_DE" );
        programRuleVariableNumericDE.setDataElement( numericDataElement );
        programRuleVariableNumericDE.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );

        ruleVariableService.addProgramRuleVariable( programRuleVariableTextAtt );
        ruleVariableService.addProgramRuleVariable( programRuleVariableNumericAtt );
        ruleVariableService.addProgramRuleVariable( programRuleVariableTextDE );
        ruleVariableService.addProgramRuleVariable( programRuleVariableNumericDE );

        programRuleTextAtt = createProgramRule( 'P', program );
        programRuleNumericAtt = createProgramRule( 'Q', program );
        programRuleTextDE = createProgramRule( 'R', program );
        programRuleNumericDE = createProgramRule( 'S', program );

        programRuleTextAtt.setCondition( conditionTextAtt );
        programRuleNumericAtt.setCondition( conditionNumericAtt );
        programRuleTextDE.setCondition( conditionTextDE );
        programRuleNumericDE.setCondition( conditionNumericDE );

        programRuleService.addProgramRule( programRuleTextAtt );
        programRuleService.addProgramRule( programRuleNumericAtt );
        programRuleService.addProgramRule( programRuleTextDE );
        programRuleService.addProgramRule( programRuleNumericDE );
    }

    @Test
    public void testProgramRuleWithTextTrackedEntityAttribute()
    {
        RuleValidationResult result = validateRuleCondition( programRuleTextAtt.getCondition(), programRuleTextAtt );
        assertNotNull( result );
        assertTrue( result.isValid() );
    }

    @Test
    public void testProgramRuleWithNumericTrackedEntityAttribute()
    {
        RuleValidationResult result = validateRuleCondition( programRuleNumericAtt.getCondition(), programRuleNumericAtt );
        assertNotNull( result );
        assertTrue( result.isValid() );
    }

    @Test
    public void testProgramRuleWithNumericTrackedEntityAttributeWithOr()
    {
        RuleValidationResult result = validateRuleCondition( conditionNumericAttWithOR, programRuleNumericAtt );
        assertNotNull( result );
        assertTrue( result.isValid() );
    }

    @Test
    public void testProgramRuleWithNumericTrackedEntityAttributeWithAnd()
    {
        RuleValidationResult result = validateRuleCondition( conditionNumericAttWithAND, programRuleNumericAtt );
        assertNotNull( result );
        assertTrue( result.isValid() );
    }

    @Test
    public void testProgramRuleWithTextDataElement()
    {
        RuleValidationResult result = validateRuleCondition( programRuleTextDE.getCondition(), programRuleTextDE );
        assertNotNull( result );
        assertTrue( result.isValid() );
    }

    @Test
    public void testProgramRuleWithNumericDataElement()
    {
        RuleValidationResult result = validateRuleCondition( programRuleNumericDE.getCondition(), programRuleNumericDE );
        assertNotNull( result );
        assertTrue( result.isValid() );
    }

    @Test
    public void testProgramRuleWithLiterals()
    {
        RuleValidationResult result = validateRuleCondition( conditionLiteralString, programRuleNumericDE );
        assertNotNull( result );
        assertTrue( result.isValid() );
    }

    @Test
    public void testIncorrectRuleWithLiterals()
    {
        RuleValidationResult result = validateRuleCondition( "1 > 2 +", programRuleTextAtt );
        assertNotNull( result );
        assertFalse( result.isValid() );
        assertThat( result.getException(), instanceOf( IllegalStateException.class ) );
    }

    @Test
    public void testIncorrectRuleWithDataElement()
    {
        RuleValidationResult result = validateRuleCondition( incorrectConditionTextDE, programRuleTextDE );
        assertNotNull( result );
        assertFalse( result.isValid() );
        assertThat( result.getException(), instanceOf( IllegalStateException.class ) );
    }

    private RuleValidationResult validateRuleCondition( String condition, ProgramRule programRule )
    {
       return programRuleEngineNew.getDescription( condition, programRule );
    }
}
