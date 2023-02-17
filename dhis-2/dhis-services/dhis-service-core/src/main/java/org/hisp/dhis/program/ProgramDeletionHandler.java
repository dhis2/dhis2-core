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
package org.hisp.dhis.program;

import static org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;
import static org.hisp.dhis.system.deletion.DeletionVeto.ACCEPT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.hisp.dhis.system.deletion.IdObjectDeletionHandler;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.UserRole;
import org.springframework.stereotype.Component;

/**
 * @author Chau Thu Tran
 */
@Component
@RequiredArgsConstructor
public class ProgramDeletionHandler extends IdObjectDeletionHandler<Program>
{
    private final ProgramService programService;

    private final CategoryService categoryService;

    @Override
    protected void registerHandler()
    {
        whenDeleting( CategoryCombo.class, this::deleteCategoryCombo );
        whenDeleting( OrganisationUnit.class, this::deleteOrganisationUnit );
        whenDeleting( UserRole.class, this::deleteUserRole );
        whenVetoing( TrackedEntityType.class, this::allowDeleteTrackedEntityType );
        whenDeleting( TrackedEntityAttribute.class, this::deleteTrackedEntityAttribute );
        whenDeleting( DataEntryForm.class, this::deleteDataEntryForm );
    }

    private void deleteCategoryCombo( CategoryCombo categoryCombo )
    {
        CategoryCombo defaultCategoryCombo = categoryService
            .getCategoryComboByName( DEFAULT_CATEGORY_COMBO_NAME );

        Collection<Program> programs = idObjectManager.getAllNoAcl( Program.class );

        for ( Program program : programs )
        {
            if ( program != null && categoryCombo.equals( program.getCategoryCombo() ) )
            {
                program.setCategoryCombo( defaultCategoryCombo );
                idObjectManager.updateNoAcl( program );
            }
        }
    }

    private void deleteOrganisationUnit( OrganisationUnit unit )
    {
        for ( Program program : unit.getPrograms() )
        {
            program.getOrganisationUnits().remove( unit );
            idObjectManager.updateNoAcl( program );
        }
    }

    private void deleteUserRole( UserRole group )
    {
        Collection<Program> programs = idObjectManager.getAllNoAcl( Program.class );

        for ( Program program : programs )
        {
            if ( program.getUserRoles().remove( group ) )
            {
                idObjectManager.updateNoAcl( program );
            }
        }
    }

    private DeletionVeto allowDeleteTrackedEntityType( TrackedEntityType trackedEntityType )
    {
        Collection<Program> programs = programService.getProgramsByTrackedEntityType( trackedEntityType );

        return (programs != null && !programs.isEmpty()) ? VETO : ACCEPT;
    }

    private void deleteTrackedEntityAttribute( TrackedEntityAttribute trackedEntityAttribute )
    {
        Collection<Program> programs = idObjectManager.getAllNoAcl( Program.class );

        for ( Program program : programs )
        {
            List<ProgramTrackedEntityAttribute> removeList = new ArrayList<>();

            for ( ProgramTrackedEntityAttribute programAttribute : program.getProgramAttributes() )
            {
                if ( programAttribute.getAttribute().equals( trackedEntityAttribute ) )
                {
                    removeList.add( programAttribute );
                }
            }

            if ( !removeList.isEmpty() )
            {
                program.getProgramAttributes().removeAll( removeList );
                idObjectManager.updateNoAcl( program );
            }
        }
    }

    private void deleteDataEntryForm( DataEntryForm dataEntryForm )
    {
        List<Program> associatedPrograms = programService.getProgramsByDataEntryForm( dataEntryForm );

        for ( Program program : associatedPrograms )
        {
            program.setDataEntryForm( null );
            idObjectManager.updateNoAcl( program );
        }
    }
}
