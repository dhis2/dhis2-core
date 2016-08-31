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

import java.util.Collection;
import java.util.List;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

/**
 * @author Chau Thu Tran
 * 
 * @version CaseAggregationConditionService.java Nov 17, 2010 10:56:29 AM
 */
public interface CaseAggregationConditionService
{
    /**
     * Adds an {@link CaseAggregationCondition}
     * 
     * @param TrackedEntityAttributeGroup The to CaseAggregationCondition add.
     * 
     * @return A generated unique id of the added
     *         {@link CaseAggregationCondition}.
     */
    int addCaseAggregationCondition( CaseAggregationCondition caseAggregationCondition );

    /**
     * Updates a {@link CaseAggregationCondition}.
     * 
     * @param TrackedEntityAttributeGroup the CaseAggregationCondition to update.
     */
    void updateCaseAggregationCondition( CaseAggregationCondition caseAggregationCondition );

    /**
     * Deletes a {@link CaseAggregationCondition}.
     * 
     * @param TrackedEntityAttributeGroup the CaseAggregationCondition to delete.
     */
    void deleteCaseAggregationCondition( CaseAggregationCondition caseAggregationCondition );

    /**
     * Returns a {@link CaseAggregationCondition}.
     * 
     * @param id the id of the CaseAggregationCondition to return.
     * 
     * @return the CaseAggregationCondition with the given id
     */
    CaseAggregationCondition getCaseAggregationCondition( int id );

    /**
     * Returns a {@link CaseAggregationCondition} with a given name.
     * 
     * @param name the name of the CaseAggregationCondition to return.
     * 
     * @return the CaseAggregationCondition with the given name, or null if no
     *         match.
     */
    CaseAggregationCondition getCaseAggregationCondition( String name );

    /**
     * Returns a {@link CaseAggregationCondition} with a given name.
     *
     * @param id the uid of the CaseAggregationCondition to return.
     *
     * @return the CaseAggregationCondition with the given uid, or null if no
     *         match.
     */
    CaseAggregationCondition getCaseAggregationConditionByUid( String id );
    
    /**
     * Returns all {@link CaseAggregationCondition}
     * 
     * @return A collection of all CaseAggregationCondition, or an empty
     *         collection if there are no CaseAggregationConditions.
     */
    Collection<CaseAggregationCondition> getAllCaseAggregationCondition();

    /**
     * Retrieve {@link CaseAggregationCondition} by a {@link DataElement}
     * 
     * @param dataElement DataElement
     * 
     * @return A collection of CaseAggregationCondition
     */
    Collection<CaseAggregationCondition> getCaseAggregationCondition( DataElement dataElement );

    /**
     * Retrieve a {@link CaseAggregationCondition} by a {@link DataElement} and
     * {@link DataElementCategoryOptionCombo}
     * 
     * @param dataElement DataElement
     * @param optionCombo DataElementCategoryOptionCombo
     * 
     * @return A CaseAggregationCondition
     */
    CaseAggregationCondition getCaseAggregationCondition( DataElement dataElement,
        DataElementCategoryOptionCombo optionCombo );

    /**
     * Retrieve a {@link CaseAggregationCondition} by a collection of
     * {@link DataElement}
     * 
     * @param dataElements DataElement collection
     * @param key The name of CaseAggregationCondition
     * @param first
     * @param max
     * 
     * @return A collection of CaseAggregationCondition
     */
    Collection<CaseAggregationCondition> getCaseAggregationConditions( Collection<DataElement> dataElements, String key, Integer first, Integer max  );

    /**
     * Retrieve a collection of {@link DataElement} by a
     * {@link CaseAggregationCondition} formula
     * 
     * @param aggregationExpression Aggregate Expression
     * 
     * @return A collection of DataElement
     */
    Collection<DataElement> getDataElementsInCondition( String aggregationExpression );

    /**
     * Retrieve a collection of {@link Program} by a
     * {@link CaseAggregationCondition} formula
     * 
     * @param aggregationExpression Aggregate Expression
     * 
     * @return A collection of Program
     */
    Collection<Program> getProgramsInCondition( String aggregationExpression );

    /**
     * Retrieve a collection of {@link TrackedEntityAttribute} by a
     * {@link CaseAggregationCondition} formula
     * 
     * @param aggregationExpression Aggregate Expression
     * 
     * @return A collection of TrackedEntityAttribute
     */
    Collection<TrackedEntityAttribute> getTrackedEntityAttributesInCondition( String aggregationExpression );

    /**
     * Retrieve the description of a {@link CaseAggregationCondition} expression
     * 
     * @param aggregationExpression Aggregate Expression
     * 
     * @return The Description of the CaseAggregationCondition
     */
    String getConditionDescription( String aggregationExpression );

    /**
     * Aggregate data values from query builder formulas defined based on
     * datasets which have data elements defined in the formulas
     * 
     * @param caseAggregateSchedule
     * @param taskStrategy Specify how to get period list based on period type
     *        of each dataset. There are four options, include last month, last
     *        3 month, last 6 month and last 12 month
     */
    void aggregate( List<CaseAggregateSchedule> caseAggregateSchedules, String taskStrategy, TaskId taskId );
    
    /**
     * Insert value aggregated from a {@link CaseAggregationCondition}
     * 
     * @param caseAggregationConditions {@link CaseAggregationCondition
     * @param orgunitIds The list of {@link OrganisationUnit} ids
     * @param periods {@link Period}
     */
    void insertAggregateValue( Collection<CaseAggregationCondition> caseAggregationConditions, Collection<Integer> orgunitIds, Collection<Period> periods );

    /**
     * Retrieve the details of each {@link DataValue} which are generated by a
     * {@link CaseAggregationCondition}
     * 
     * @param caseAggregationCondition CaseAggregationCondition
     * @param orgunitIds The list of {@link OrganisationUnit} ids
     * @param period {@link Period}
     * @param format I18nFormat
     * @param i18n I18n
     */
    Grid getAggregateValueDetails( CaseAggregationCondition aggregationCondition, OrganisationUnit orgunit,
        Period period, I18nFormat format, I18n i18n );

    /**
     * Convert an expression of {@link CaseAggregationCondition} to standard
     * query
     * 
     * @param isInsert True if converting the expression for inserting
     *        {@link DataValue}
     * @param caseExpression The expression of CaseAggregationCondition
     * @param operator There are six operators, includes COUNT, TIMES, SUM, AVG,
     *        MIN and MAX
     * @param aggregateDeId The aggregate data element which is used for saving
     *        a datavalue
     * @param aggregateDeName The name of aggregate data element
     * @param optionComboId The {@link DataElementCategoryOptionCombo} which is
     *        used for saving a datavalue
     * @param optionComboName The name ofDataElementCategoryOptionCombo
     * @param deSumId The id of the data element
     * @param orgunitIds The ids of orgunits where data are retrieved to
     *        calculate value
     * @param period The period for retrieving data
     * 
     * @return SQL
     */
    String parseExpressionToSql( boolean isInsert, String caseExpression, String operator, Integer aggregateDeId,
        String aggregateDeName, Integer optionComboId, String optionComboName, Integer deSumId,
        Collection<Integer> orgunitIds, Period period );

    /**
     * Convert an expression of {@link CaseAggregationCondition} to standard
     * query
     * 
     * @param caseExpression The expression of CaseAggregationCondition
     * @param operator There are six operators, includes COUNT, TIMES, SUM, AVG,
     *        MIN and MAX
     * @param orgunitIds The id of {@link OrganisationUnit}
     * @param period The period for retrieving data
     * 
     * @return SQL
     */
    String parseExpressionDetailsToSql( String caseExpression, String operator, Integer orgunitId, Period period );

    /**
     * Get list of {@link TrackedEntityInstance} ids from SQL
     * 
     * @param sql SQL statement
     * 
     * @return List of TrackedEntityInstance ids
     */
    List<Integer> executeSQL( String sql );
   
    /**
     * @param dataElements
     * @return
     */
    int countCaseAggregationCondition( Collection<DataElement> dataElements, String key );
    
    /**
     * Return a data value table aggregated of a query builder formula
     * 
     * @param caseAggregationConditions The collection of query builder expressions
     * @param orgunitIds The ids of organisation unit where to aggregate data
     *        value
     * @param period The collections of date ranges for aggregate data value
     * @param format
     * @param i18n
     */
    List<Grid> getAggregateValue( Collection<CaseAggregationCondition> caseAggregationConditions, Collection<Integer> orgunitIds,
        Collection<Period> periods, I18nFormat format, I18n i18n );

}
