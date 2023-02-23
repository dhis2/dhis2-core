/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.program.Program;

@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class CategorySecurityUtils
{
    /**
     * Returns the categories the user is constrained to. If the user is super
     * user, an empty set is returned. If the user is not super user, the
     * categories of the program category combo are returned if present.
     *
     * @param program the program to get the categories from
     * @param dimensionalObjects the dimensional objects
     * @return the categories the user is constrained to.
     */
    public static Collection<Category> getCategoriesWithoutRestrictions( Program program,
        List<DimensionalObject> dimensionalObjects )
    {
        List<Program> programs = Optional.ofNullable( program )
            .map( Collections::singletonList )
            .orElse( Collections.emptyList() );
        return getCategoriesWithoutRestrictions( programs, dimensionalObjects );
    }

    /**
     * Returns the categories the user is constrained to. If the user is super
     * user, an empty set is returned. If the user is not super user, the
     * categories of the program category combo are returned if present.
     *
     * @param programs the programs to get the categories from
     * @param dimensionalObjects the dimensional objects
     * @return the categories the user is constrained to.
     */
    public static Collection<Category> getCategoriesWithoutRestrictions( List<Program> programs,
        List<DimensionalObject> dimensionalObjects )
    {
        return programs.stream()
            .filter( Program::hasNonDefaultCategoryCombo )
            .map( Program::getCategoryCombo )
            .map( CategoryCombo::getCategories )
            .flatMap( Collection::stream )
            /*
             * If the user has selected a category option, we do not want to
             * apply any constraints
             */
            .filter( category -> !hasUserSelectedCategoryOption( category, dimensionalObjects ) )
            .collect( Collectors.toList() );
    }

    /**
     * Returns true if the user has selected a category option for the given
     * category.
     *
     * @param category the category to check
     * @param dimensionalObjects the dimensional objects
     * @return true if the user has selected a category option for the given
     *         category.
     */
    private static boolean hasUserSelectedCategoryOption( Category category,
        List<DimensionalObject> dimensionalObjects )
    {
        return dimensionalObjects.stream()
            .anyMatch( dimensionalObject -> hasUserConstraints( dimensionalObject, category ) );
    }

    /**
     * Returns true if the given dimensionalObject contains any constraint on
     * the given Category
     *
     * @param dimensionalObject the dimensional object to check
     * @param category the category to check
     * @return true if the given dimensionalObject contains any constraint on
     *         the given Category
     */
    private static boolean hasUserConstraints( DimensionalObject dimensionalObject, Category category )
    {
        return dimensionalObject.getDimensionType() == DimensionType.CATEGORY &&
            dimensionalObject.getUid().equals( category.getUid() ) &&
            !dimensionalObject.getItems().isEmpty();
    }
}
