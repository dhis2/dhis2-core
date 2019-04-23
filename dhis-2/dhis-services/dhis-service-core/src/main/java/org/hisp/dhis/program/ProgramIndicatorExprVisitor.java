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

import org.hisp.dhis.common.StringRange;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.parser.expression.*;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.util.Assert;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.parser.expression.ParserUtils.*;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.*;

/**
 * Visitor to the nodes in a parsed program indicator expression.
 * <p/>
 * Uses the ANTLR4 visitor partern.
 *
 * @author Markus Bekken
 * @author Jim Grace
 */
public class ProgramIndicatorExprVisitor
    extends ExprVisitor
{
    private ProgramIndicatorService programIndicatorService;

    private ProgramStageService programStageService;

    private DataElementService dataElementService;

    private TrackedEntityAttributeService attributeService;

    private RelationshipTypeService relationshipTypeService;

    private StatementBuilder statementBuilder;

    private I18n i18n;

    private Map<String, String> itemDescriptions = new HashMap<>();

    private ProgramIndicator programIndicator;

    private Date reportingStartDate;

    private Date reportingEndDate;

    private Set<String> dataElementAndAttributeIdentifiers;

    public static final double DEFAULT_DOUBLE_VALUE = 1d;

    public static final String DEFAULT_DATE_VALUE = "2017-07-08";

    public ProgramIndicatorExprVisitor( Map<Integer, ExprFunction> functionMap,
        Map<Integer, ExprItem> itemMap, ExprFunctionMethod functionMethod,
        ExprItemMethod itemMethod,
        ProgramIndicatorService programIndicatorService,
        ConstantService constantService,
        ProgramStageService programStageService,
        DataElementService dataElementService,
        TrackedEntityAttributeService attributeService,
        RelationshipTypeService relationshipTypeService,
        StatementBuilder statementBuilder,
        I18n i18n )
    {
        this.functionMap = checkNotNull( functionMap );
        this.itemMap = checkNotNull( itemMap );
        this.functionMethod = checkNotNull( functionMethod );
        this.itemMethod = checkNotNull( itemMethod );

        this.programIndicatorService = checkNotNull( programIndicatorService );
        this.constantService = checkNotNull( constantService );
        this.programStageService = checkNotNull( programStageService );
        this.dataElementService = checkNotNull( dataElementService );
        this.attributeService = checkNotNull( attributeService );
        this.relationshipTypeService = checkNotNull( relationshipTypeService );
        this.statementBuilder = checkNotNull( statementBuilder );
        this.i18n = checkNotNull( i18n );
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public ValueType validateStageDataElement( String text, String programStageId, String dataElementId )
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

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public ProgramIndicatorService getProgramIndicatorService()
    {
        return programIndicatorService;
    }

    public ProgramStageService getProgramStageService()
    {
        return programStageService;
    }

    public DataElementService getDataElementService()
    {
        return dataElementService;
    }

    public TrackedEntityAttributeService getAttributeService()
    {
        return attributeService;
    }

    public RelationshipTypeService getRelationshipTypeService()
    {
        return relationshipTypeService;
    }

    public StatementBuilder getStatementBuilder()
    {
        return statementBuilder;
    }

    public I18n getI18n()
    {
        return i18n;
    }

    public Map<String, String> getItemDescriptions()
    {
        return itemDescriptions;
    }

    public ProgramIndicator getProgramIndicator()
    {
        return programIndicator;
    }

    public void setProgramIndicator(
        ProgramIndicator programIndicator )
    {
        this.programIndicator = programIndicator;
    }

    public Date getReportingStartDate()
    {
        return reportingStartDate;
    }

    public void setReportingStartDate( Date reportingStartDate )
    {
        this.reportingStartDate = reportingStartDate;
    }

    public Date getReportingEndDate()
    {
        return reportingEndDate;
    }

    public void setReportingEndDate( Date reportingEndDate )
    {
        this.reportingEndDate = reportingEndDate;
    }

    public Set<String> getDataElementAndAttributeIdentifiers()
    {
        return dataElementAndAttributeIdentifiers;
    }

    public void setDataElementAndAttributeIdentifiers(
        Set<String> dataElementAndAttributeIdentifiers )
    {
        this.dataElementAndAttributeIdentifiers = dataElementAndAttributeIdentifiers;
    }
}
