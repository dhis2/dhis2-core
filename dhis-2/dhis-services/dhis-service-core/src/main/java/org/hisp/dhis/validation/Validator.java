package org.hisp.dhis.validation;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.system.util.SystemUtils;
import org.springframework.context.ApplicationContext;

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
     * 
     * @return a collection of any validations that were found
     */
    public static Collection<ValidationResult> validate( ValidationRunContext context, 
        ApplicationContext applicationContext )
    {
        DataElementCategoryService categoryService = (DataElementCategoryService) 
            applicationContext.getBean( DataElementCategoryService.class );
                
        int threadPoolSize = getThreadPoolSize( context );
        ExecutorService executor = Executors.newFixedThreadPool( threadPoolSize );

        for ( OrganisationUnitExtended sourceX : context.getSourceXs() )
        {
            if ( sourceX.getToBeValidated() )
            {
                ValidationTask task = (ValidationTask) applicationContext.getBean( DataValidationTask.NAME );
                task.init( sourceX, context );
                
                executor.execute( task );
            }
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

        return context.getValidationResults();
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

        if ( threadPoolSize > context.getCountOfSourcesToValidate() )
        {
            threadPoolSize = context.getCountOfSourcesToValidate();
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
        DataElementCategoryService dataElementCategoryService )
    {
        for ( ValidationResult result : results )
        {
            result.setAttributeOptionCombo( dataElementCategoryService
                .getDataElementCategoryOptionCombo( result.getAttributeOptionCombo().getId() ) );
        }
    }
}
