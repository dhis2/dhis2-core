package org.hisp.dhis.caseaggregation;

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

import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;

/**
 * @author Chau Thu Tran
 * 
 * @version CaseAggregationCondition.java Nov 17, 2010 10:47:12 AM
 */
public class CaseAggregationCondition
    extends BaseNameableObject
{
    public static final String SEPARATOR_ID = "\\.";

    public static final String SEPARATOR_OBJECT = ":";

    public static final String AGGRERATION_COUNT = "COUNT";

    public static final String AGGRERATION_SUM = "times";

    public static final String AGGRERATION_SUM_VALUE = "sum";

    public static final String AGGRERATION_AVG_VALUE = "avg";

    public static final String AGGRERATION_AVG_MIN = "min";

    public static final String AGGRERATION_AVG_MAX = "max";

    public static final String OPERATOR_AND = "AND";

    public static final String OPERATOR_OR = "OR";

    public static String OBJECT_PROGRAM_STAGE_DATAELEMENT = "DE";

    public static String OBJECT_TRACKED_ENTITY_ATTRIBUTE = "CA";

    public static String OBJECT_PROGRAM_PROPERTY = "PP";

    public static String OBJECT_PROGRAM = "PG";

    public static String OBJECT_PROGRAM_STAGE = "PS";

    public static String OBJECT_PROGRAM_STAGE_PROPERTY = "PSP";

    public static String OBJECT_TRACKED_ENTITY_PROGRAM_STAGE_PROPERTY = "PC";

    public static String OBJECT_ORGUNIT_COMPLETE_PROGRAM_STAGE = "PSIC";
   
    public static String FORMULA_AGE = "age";
    
    public static String FORMULA_VISIT = "visit";
    
    public static String OBJECT_PROGRAM_PROPERTY_INCIDENT_DATE = "incidentDate";

    public static String OBJECT_PROGRAM_PROPERTY_ENROLLEMENT_DATE = "enrollmentDate";

    public static String OBJECT_PROGRAM_PROPERTY_REPORT_DATE = "executionDate";

    public static String MINUS_OPERATOR = "DATEDIFF";
    
    public static String MINUS_DATAELEMENT_OPERATOR_TYPE_ONE = "DEDATEDIFF_TYPE_ONE";
    
    public static String MINUS_DATAELEMENT_OPERATOR_TYPE_TWO = "DEDATEDIFF_TYPE_TWO";
    
    public static String MINUS_2DATAELEMENT_OPERATOR = "DE2DATEDIFF";
    
    public static String MINUS_2ATTRIBUTE_OPERATOR = "ATTR2DATEDIFF";

    public static String MINUS_ATTRIBUTE_OPERATOR_TYPE_ONE = "ATTRDATEDIFF_TYPE_ONE";
    
    public static String MINUS_ATTRIBUTE_OPERATOR_TYPE_TWO = "ATTRDATEDIFF_TYPE_TWO";
    
    public static String CURRENT_DATE = "current_date";
    
    public static String AUTO_STORED_BY = "aggregated_from_tracker";
    
    public static final String PARAM_PERIOD_START_DATE = "PERIOD_START_DATE";
    public static final String PARAM_PERIOD_END_DATE = "PARAM_PERIOD_END_DATE";
    public static final String PARAM_PERIOD_ID = "PERIOD_ID";
    public static final String PARAM_PERIOD_ISO_DATE = "PERIOD_ISO_DATE";

    public static final String regExp = "\\[(" + OBJECT_ORGUNIT_COMPLETE_PROGRAM_STAGE + "|" + OBJECT_PROGRAM + "|"
        + OBJECT_PROGRAM_STAGE_PROPERTY + "|" + OBJECT_PROGRAM_STAGE + "|"
        + OBJECT_TRACKED_ENTITY_PROGRAM_STAGE_PROPERTY + "|" + OBJECT_PROGRAM_STAGE_DATAELEMENT + "|"
        + OBJECT_TRACKED_ENTITY_ATTRIBUTE + "|" + OBJECT_PROGRAM_PROPERTY + ")" + SEPARATOR_OBJECT
        + "([a-zA-Z0-9@#\\- ]+[" + SEPARATOR_ID + "[a-zA-Z0-9]*]*)" + "\\]";

    // Date dataElement - currentDate/incidentDate/executionDate/enrollmentDate
    public static final String minusDataelementRegExp1 = MINUS_OPERATOR + "{1}\\s*\\(\\s*\\["
        + OBJECT_PROGRAM_STAGE_DATAELEMENT + SEPARATOR_OBJECT + "([0-9]+)+" + SEPARATOR_ID + "([0-9]+)+" + SEPARATOR_ID
        + "([0-9]+)+\\]\\s*(,)\\s*" + "(" + CURRENT_DATE + "|" + OBJECT_PROGRAM_PROPERTY_INCIDENT_DATE + "|"
        + OBJECT_PROGRAM_PROPERTY_ENROLLEMENT_DATE + "|" + OBJECT_PROGRAM_PROPERTY_REPORT_DATE
        + ")\\s*\\)\\s*(>=|<=|!=|>|<|=){1}\\s*([0-9]+){1}";

    // currentDate/incidentDate/executionDate/enrollmentDate - Date dataElement
    public static final String minusDataelementRegExp2 = MINUS_OPERATOR + "{1}\\s*\\(\\s*(" + CURRENT_DATE + "|"
        + OBJECT_PROGRAM_PROPERTY_INCIDENT_DATE + "|" + OBJECT_PROGRAM_PROPERTY_ENROLLEMENT_DATE + "|"
        + OBJECT_PROGRAM_PROPERTY_REPORT_DATE + ")\\s*(,)\\s*\\[" + OBJECT_PROGRAM_STAGE_DATAELEMENT + SEPARATOR_OBJECT
        + "([0-9]+)+" + SEPARATOR_ID + "([0-9]+)+" + SEPARATOR_ID
        + "([0-9]+)+\\]\\s*\\)\\s*(>=|<=|!=|>|<|=){1}\\s*([0-9]+){1}";

    // Date dataElement - Date dataElement
    public static final String minus2DataelementRegExp = MINUS_OPERATOR + "{1}\\s*\\(\\s*(\\["
        + OBJECT_PROGRAM_STAGE_DATAELEMENT + SEPARATOR_OBJECT + "([0-9]+" + SEPARATOR_ID + "[0-9]+" + SEPARATOR_ID
        + "[0-9]+)+\\])\\s*(,)\\s*(\\[" + OBJECT_PROGRAM_STAGE_DATAELEMENT + SEPARATOR_OBJECT + "([0-9]+"
        + SEPARATOR_ID + "[0-9]+" + SEPARATOR_ID + "[0-9]+)+\\])\\s*\\)\\s*(>=|<=|!=|>|<|=){1}\\s*([0-9]+)";

    // currentDate/ incidentDate/executionDate/enrollmentDate - Date attribute
    public static final String minusAttributeRegExp1 = MINUS_OPERATOR + "{1}\\s*\\(\\s*(" + CURRENT_DATE + "|"
        + OBJECT_PROGRAM_PROPERTY_INCIDENT_DATE + "|" + OBJECT_PROGRAM_PROPERTY_ENROLLEMENT_DATE + "|"
        + OBJECT_PROGRAM_PROPERTY_REPORT_DATE + ")\\s*(,)\\s*\\[" + OBJECT_TRACKED_ENTITY_ATTRIBUTE + SEPARATOR_OBJECT
        + "([0-9]+)+\\]\\s*\\)\\s(>=|<=|!=|>|<|=){1}\\s*([0-9]+){1}";

    // Date attribute - currentDate/ incidentDate/executionDate/enrollmentDate
    public static final String minusAttributeRegExp2 = MINUS_OPERATOR + "{1}\\s*\\(\\s*\\["
        + OBJECT_TRACKED_ENTITY_ATTRIBUTE + SEPARATOR_OBJECT + "([0-9]+)+\\]\\s*(,)\\s*(" + CURRENT_DATE + "|"
        + OBJECT_PROGRAM_PROPERTY_INCIDENT_DATE + "|" + OBJECT_PROGRAM_PROPERTY_ENROLLEMENT_DATE + "|"
        + OBJECT_PROGRAM_PROPERTY_REPORT_DATE + ")\\s*\\)\\s*(>=|<=|!=|>|<|=){1}\\s*([0-9]+){1}";

    // Date attribute - Date attribute
    public static final String minus2AttributeRegExp = MINUS_OPERATOR + "{1}\\s*\\(\\s*(\\["
        + OBJECT_TRACKED_ENTITY_ATTRIBUTE + SEPARATOR_OBJECT + "([0-9]+)+\\])\\s*(,)\\s*(\\["
        + OBJECT_TRACKED_ENTITY_ATTRIBUTE + SEPARATOR_OBJECT + "([0-9]+)+\\])\\s*\\)\\s*(>=|<=|!=|>|<|=){1}\\s*([0-9]+)";
    
    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private String operator;

    private String aggregationExpression;

    private DataElement aggregationDataElement;

    private DataElementCategoryOptionCombo optionCombo;

    private DataElement deSum;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public CaseAggregationCondition()
    {

    }

    public CaseAggregationCondition( String name, String operator, String aggregationExpression,
        DataElement aggregationDataElement, DataElementCategoryOptionCombo optionCombo )
    {
        this.name = name;
        this.operator = operator;
        this.aggregationExpression = aggregationExpression;
        this.aggregationDataElement = aggregationDataElement;
        this.optionCombo = optionCombo;
    }

    public CaseAggregationCondition( String name, String operator, String aggregationExpression,
        DataElement aggregationDataElement, DataElementCategoryOptionCombo optionCombo, DataElement deSum )
    {
        this.name = name;
        this.operator = operator;
        this.aggregationExpression = aggregationExpression;
        this.aggregationDataElement = aggregationDataElement;
        this.optionCombo = optionCombo;
        this.deSum = deSum;
    }

    // -------------------------------------------------------------------------
    // Logical
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((aggregationExpression == null) ? 0 : aggregationExpression.hashCode());
        result = prime * result + ((operator == null) ? 0 : operator.hashCode());
        return result;
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null )
        {
            return false;
        }

        if ( !getClass().isAssignableFrom( object.getClass() ) )
        {
            return false;
        }

        final CaseAggregationCondition other = (CaseAggregationCondition) object;

        if ( aggregationExpression == null )
        {
            if ( other.aggregationExpression != null )
            {
                return false;
            }
        }
        else if ( !aggregationExpression.equals( other.aggregationExpression ) )
        {
            return false;
        }

        if ( operator == null )
        {
            if ( other.operator != null )
            {
                return false;
            }
        }
        else if ( !operator.equals( other.operator ) )
        {
            return false;
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Getters && Setters
    // -------------------------------------------------------------------------

    public DataElement getAggregationDataElement()
    {
        return aggregationDataElement;
    }

    public DataElementCategoryOptionCombo getOptionCombo()
    {
        return optionCombo;
    }

    public void setOptionCombo( DataElementCategoryOptionCombo optionCombo )
    {
        this.optionCombo = optionCombo;
    }

    public void setAggregationDataElement( DataElement aggregationDataElement )
    {
        this.aggregationDataElement = aggregationDataElement;
    }

    public String getOperator()
    {
        return operator;
    }

    public void setOperator( String operator )
    {
        this.operator = operator;
    }

    public void setId( Integer id )
    {
        this.id = id;
    }

    public String getAggregationExpression()
    {
        return aggregationExpression;
    }

    public void setAggregationExpression( String aggregationExpression )
    {
        this.aggregationExpression = aggregationExpression;
    }

    public DataElement getDeSum()
    {
        return deSum;
    }

    public void setDeSum( DataElement deSum )
    {
        this.deSum = deSum;
    }
}
