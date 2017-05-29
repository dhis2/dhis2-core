package org.hisp.dhis.analytics.utils;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Utils for analytics tests
 *
 * @author Henning Haakonsen
 */
public interface AnalyticsTestUtils
{
    /**
     * Configure org unit hierarchy like so:
     *
     *          A
     *         / \
     *        B   C
     *       / \
     *      D   E
     *
     * @param ouA root
     * @param ouB leftRoot
     * @param ouC rightRoot
     * @param ouD leftB
     * @param ouE rightB
     */
    void configureHierarchy( OrganisationUnit ouA, OrganisationUnit ouB,
        OrganisationUnit ouC, OrganisationUnit ouD, OrganisationUnit ouE );

    /**
     * Reads CSV input file.
     *
     * @param inputFile points to file in class path
     * @return list of list of strings
     */
    ArrayList<String[]> readInputFile( String inputFile )
        throws IOException;

    /**
     * Test if values from keyValue corresponds with values in
     * aggregatedResultMapping. Also test for null values.
     *
     * @param aggregatedResultData aggregated results
     * @param keyValue             expected results
     */
    void assertResultGrid( Grid aggregatedResultData, Map<String, Double> keyValue );

    /**
     * Test if values from keyValue corresponds with values in
     * aggregatedDataValueMapping. Also test for null values, and "" as key in
     * aggregatedDataValueMapping
     *
     * @param aggregatedResultMapping aggregated values
     * @param keyValue                   expected results
     */
    void assertResultMapping( Map<String, Object> aggregatedResultMapping,
        Map<String, Double> keyValue );

    /**
     * Test if values from keyValue corresponds with values in
     * aggregatedDataValueSet. Also test for null values.
     *
     * @param aggregatedResultSet aggregated values
     * @param keyValue               expected results
     */
    void assertResultSet( DataValueSet aggregatedResultSet, Map<String, Double> keyValue );
}
