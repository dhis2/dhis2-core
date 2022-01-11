/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.Lists;

/**
 * Evaluates validation rules.
 *
 * @author Jim Grace
 */
public class Validator
{
    /**
     * Evaluates validation rules for a collection of organisation units. This
     * method breaks the job down by organisation unit. It assigns the
     * evaluation for each organisation unit to a task that can be evaluated
     * independently in a multi-threaded environment.
     * <p/>
     * Return early with no results if there are no organisation units or no
     * validation rules.
     *
     * @return a collection of any validations that were found
     */
    public static List<ValidationResult> validate( ValidationRunContext context,
        ApplicationContext applicationContext, AnalyticsService analyticsService )
    {
        CategoryService categoryService = applicationContext.getBean( CategoryService.class );

        int threadPoolSize = getThreadPoolSize( context );

        if ( threadPoolSize == 0 || context.getPeriodTypeXs().isEmpty() )
        {
            return new ArrayList<>( context.getValidationResults() );
        }

        ExecutorService executor = Executors.newFixedThreadPool( threadPoolSize );

        List<List<OrganisationUnit>> orgUnitLists = Lists.partition( context.getOrgUnits(),
            ValidationRunContext.ORG_UNITS_PER_TASK );

        for ( List<OrganisationUnit> orgUnits : orgUnitLists )
        {
            ValidationTask task = (ValidationTask) applicationContext.getBean( DataValidationTask.NAME );
            task.init( orgUnits, context, analyticsService );

            executor.execute( task );
        }

        executor.shutdown();

        try
        {
            executor.awaitTermination( 6, TimeUnit.HOURS );
        }
        catch ( InterruptedException e )
        {
            executor.shutdownNow();
        }

        reloadAttributeOptionCombos( context.getValidationResults(), categoryService );

        return new ArrayList<>( context.getValidationResults() );
    }

    /**
     * Determines how many threads we should use for testing validation rules.
     *
     * @param context validation run context
     * @return number of threads we should use for testing validation rules
     */
    private static int getThreadPoolSize( ValidationRunContext context )
    {
        int threadPoolSize = SystemUtils.getCpuCores();

        if ( threadPoolSize > 2 )
        {
            threadPoolSize--;
        }

        int numberOfTasks = context.getNumberOfTasks();

        if ( threadPoolSize > numberOfTasks )
        {
            threadPoolSize = numberOfTasks;
        }

        return threadPoolSize;
    }

    /**
     * Reload attribute category option combos into this Hibernate context.
     *
     * @param results
     * @param dataElementCategoryService
     */
    private static void reloadAttributeOptionCombos( Collection<ValidationResult> results,
        CategoryService dataElementCategoryService )
    {
        for ( ValidationResult result : results )
        {
            result.setAttributeOptionCombo( dataElementCategoryService
                .getCategoryOptionCombo( result.getAttributeOptionCombo().getId() ) );
        }
    }
}
