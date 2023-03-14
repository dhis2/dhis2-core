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
package org.hisp.dhis.analytics.tei.query;

import static org.hisp.dhis.analytics.common.CommonQueryRequest.DEFAULT_ORG_UNIT_SELECTION_MODE;
import static org.hisp.dhis.analytics.common.ValueTypeMapping.STRING;
import static org.hisp.dhis.analytics.common.query.RenderableHelper.FALSE_CONDITION;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.hisp.dhis.commons.collection.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.common.params.CommonParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.BaseRenderable;
import org.hisp.dhis.analytics.common.query.BinaryConditionRenderer;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.OrCondition;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * Provides methods responsible for generating SQL statements on top of
 * organization units, for events, enrollments and teis.
 */
@RequiredArgsConstructor( staticName = "of" )
public class OrganisationUnitCondition extends BaseRenderable
{
    private static final Collection<OrganisationUnitSelectionMode> ACCEPTED_OU_MODES = List.of(
        DESCENDANTS,
        CHILDREN,
        SELECTED );

    public static final String OULEVEL = "uidlevel";

    private final DimensionIdentifier<DimensionParam> dimensionIdentifier;

    private final QueryContext queryContext;

    /**
     * Renders the org. unit SQL conditions for a given enrollment. The SQL
     * output will look like:
     *
     * "ou" = :1
     *
     * @return the SQL statement
     */
    @Nonnull
    @Override
    public String render()
    {
        return getCondition().render();
    }

    public Renderable getCondition()
    {
        OrganisationUnitSelectionMode ouMode = getOuMode();

        List<OrganisationUnit> organisationUnits = getOrganisationUnits();

        if ( ouMode == SELECTED )
        {

            List<String> items = organisationUnits.stream()
                .map( BaseIdentifiableObject::getUid )
                .collect( Collectors.toList() );

            return isEmpty( items ) ? FALSE_CONDITION
                : BinaryConditionRenderer.of(
                    Field.ofDimensionIdentifier( dimensionIdentifier ),
                    IN,
                    items,
                    STRING,
                    queryContext );

        }
        else if ( ouMode == CHILDREN )
        {

            List<String> items = organisationUnits.stream()
                .map( OrganisationUnit::getChildren )
                .flatMap( Collection::stream )
                .map( BaseIdentifiableObject::getUid )
                .collect( Collectors.toList() );

            return isEmpty( items ) ? FALSE_CONDITION
                : BinaryConditionRenderer.of(
                    Field.ofDimensionIdentifier( dimensionIdentifier ),
                    IN,
                    items,
                    STRING,
                    queryContext );
        }

        // ouMode = Descendants
        List<Renderable> orgUnitConditions = new ArrayList<>();

        for ( OrganisationUnit organisationUnit : organisationUnits )
        {
            orgUnitConditions.add( BinaryConditionRenderer.of(
                Field.ofRenamedDimensionIdentifier(
                    dimensionIdentifier,
                    OULEVEL + organisationUnit.getLevel() ),
                IN,
                List.of( organisationUnit.getUid() ),
                STRING,
                queryContext ) );
        }

        return isEmpty( orgUnitConditions ) ? FALSE_CONDITION : OrCondition.of( orgUnitConditions );
    }

    private OrganisationUnitSelectionMode getOuMode()
    {
        return Optional.of( queryContext )
            .map( QueryContext::getTeiQueryParams )
            .map( TeiQueryParams::getCommonParams )
            .map( CommonParams::getOuMode )
            .filter( ACCEPTED_OU_MODES::contains )
            .orElse( DEFAULT_ORG_UNIT_SELECTION_MODE );
    }

    private List<OrganisationUnit> getOrganisationUnits()
    {
        return Optional.of( dimensionIdentifier )
            .map( DimensionIdentifier::getDimension )
            .map( DimensionParam::getDimensionalObject )
            .map( DimensionalObject::getItems )
            .orElse( Collections.emptyList() )
            .stream()
            .map( OrganisationUnit.class::cast )
            .collect( Collectors.toList() );
    }
}
