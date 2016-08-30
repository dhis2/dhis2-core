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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.expression.Operator;

/**
 * @author Chau Thu Tran
 */
@JacksonXmlRootElement( localName = "programValidation", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramValidation
    extends BaseIdentifiableObject
{
    public static final String SEPARATOR_OBJECT = ":";

    public static String OBJECT_PROGRAM_STAGE_DATAELEMENT = "DE";

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private ProgramExpression leftSide;

    private Operator operator;

    private ProgramExpression rightSide;

    private Program program;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ProgramValidation()
    {

    }

    public ProgramValidation( String name, ProgramExpression leftSide, ProgramExpression rightSide,
        Program program )
    {
        this.name = name;
        this.leftSide = leftSide;
        this.rightSide = rightSide;
        this.program = program;
    }

    // -------------------------------------------------------------------------
    // Getters && Setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramExpression getLeftSide()
    {
        return leftSide;
    }

    public void setLeftSide( ProgramExpression leftSide )
    {
        this.leftSide = leftSide;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramExpression getRightSide()
    {
        return rightSide;
    }

    public void setRightSide( ProgramExpression rightSide )
    {
        this.rightSide = rightSide;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Program getProgram()
    {
        return program;
    }

    public void setProgram( Program program )
    {
        this.program = program;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Operator getOperator()
    {
        return operator;
    }

    public void setOperator( Operator operator )
    {
        this.operator = operator;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            ProgramValidation programValidation = (ProgramValidation) other;

            if ( mergeMode.isReplace() )
            {
                leftSide = programValidation.getLeftSide();
                operator = programValidation.getOperator();
                rightSide = programValidation.getRightSide();
                program = programValidation.getProgram();
            }
            else if ( mergeMode.isMerge() )
            {
                leftSide = programValidation.getLeftSide() == null ? leftSide : programValidation.getLeftSide();
                rightSide = programValidation.getRightSide() == null ? rightSide : programValidation.getRightSide();
                operator = programValidation.getOperator() == null ? operator : programValidation.getOperator();
                program = programValidation.getProgram() == null ? program : programValidation.getProgram();
            }
        }
    }
}
