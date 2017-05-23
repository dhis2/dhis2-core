package org.hisp.dhis.analytics.utils;

import org.hisp.dhis.organisationunit.OrganisationUnit;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by henninghakonsen on 23/05/2017.
 * Project: dhis-2.
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
    ArrayList<String[]> readInputFile( String inputFile ) throws IOException;
}
