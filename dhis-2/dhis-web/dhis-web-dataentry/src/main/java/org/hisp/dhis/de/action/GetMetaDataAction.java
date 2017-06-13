package org.hisp.dhis.de.action;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import static org.hisp.dhis.commons.util.TextUtils.SEP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;
import com.opensymphony.xwork2.Action;

/**
 * @author Lars Helge Overland
 */
public class GetMetaDataAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    private IndicatorService indicatorService;

    public void setIndicatorService( IndicatorService indicatorService )
    {
        this.indicatorService = indicatorService;
    }

    private ExpressionService expressionService;

    public void setExpressionService( ExpressionService expressionService )
    {
        this.expressionService = expressionService;
    }

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }
    
    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private IdentifiableObjectManager identifiableObjectManager;
    
    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private Collection<DataElement> significantZeros;

    public Collection<DataElement> getSignificantZeros()
    {
        return significantZeros;
    }

    private Collection<DataElement> dataElements;

    public Collection<DataElement> getDataElements()
    {
        return dataElements;
    }

    private List<DataElement> dataElementsWithOptionSet = new ArrayList<>();

    public List<DataElement> getDataElementsWithOptionSet()
    {
        return dataElementsWithOptionSet;
    }

    private Collection<Indicator> indicators;

    public Collection<Indicator> getIndicators()
    {
        return indicators;
    }

    private List<DataSet> dataSets;

    public List<DataSet> getDataSets()
    {
        return dataSets;
    }

    private boolean emptyOrganisationUnits;

    public boolean isEmptyOrganisationUnits()
    {
        return emptyOrganisationUnits;
    }

    private List<DataElementCategoryCombo> categoryCombos;

    public List<DataElementCategoryCombo> getCategoryCombos()
    {
        return categoryCombos;
    }

    private List<DataElementCategory> categories;

    public List<DataElementCategory> getCategories()
    {
        return categories;
    }

    private DataElementCategoryCombo defaultCategoryCombo;

    public DataElementCategoryCombo getDefaultCategoryCombo()
    {
        return defaultCategoryCombo;
    }

    private Map<String, List<DataElementCategoryOption>> categoryOptionMap = new HashMap<>();

    public Map<String, List<DataElementCategoryOption>> getCategoryOptionMap()
    {
        return categoryOptionMap;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        User user = currentUserService.getCurrentUser();

        Date lastUpdated = DateUtils.max( Sets.newHashSet(
            identifiableObjectManager.getLastUpdated( DataElement.class ), 
            identifiableObjectManager.getLastUpdated( OptionSet.class ),
            identifiableObjectManager.getLastUpdated( Indicator.class ),
            identifiableObjectManager.getLastUpdated( DataSet.class ),
            identifiableObjectManager.getLastUpdated( DataElementCategoryCombo.class ),
            identifiableObjectManager.getLastUpdated( DataElementCategory.class ),
            identifiableObjectManager.getLastUpdated( DataElementCategoryOption.class ) ) );
        String tag = lastUpdated != null && user != null ? ( DateUtils.getLongDateString( lastUpdated ) + SEP + user.getUid() ): null;
        
        if ( ContextUtils.isNotModified( ServletActionContext.getRequest(), ServletActionContext.getResponse(), tag ) )
        {
            return SUCCESS;
        }
                
        if ( user != null && user.getOrganisationUnits().isEmpty() )
        {
            emptyOrganisationUnits = true;

            return SUCCESS;
        }

        significantZeros = dataElementService.getDataElementsByZeroIsSignificant( true );

        dataElements = dataElementService.getDataElementsWithDataSets();

        for ( DataElement dataElement : dataElements )
        {
            if ( dataElement != null && dataElement.getOptionSet() != null )
            {
                dataElementsWithOptionSet.add( dataElement );
            }
        }

        indicators = indicatorService.getIndicatorsWithDataSets();

        expressionService.substituteExpressions( indicators, null );

        dataSets = dataSetService.getCurrentUserDataSets();
        
        Set<DataElementCategoryCombo> categoryComboSet = new HashSet<>();
        Set<DataElementCategory> categorySet = new HashSet<>();

        for ( DataSet dataSet : dataSets )
        {
            if ( dataSet.getCategoryCombo() != null )
            {
                categoryComboSet.add( dataSet.getCategoryCombo() );
            }
        }

        for ( DataElementCategoryCombo categoryCombo : categoryComboSet )
        {
            if ( categoryCombo.getCategories() != null )
            {
                categorySet.addAll( categoryCombo.getCategories() );
            }
        }

        categoryCombos = new ArrayList<>( categoryComboSet );
        categories = new ArrayList<>( categorySet );

        for ( DataElementCategory category : categories )
        {
            List<DataElementCategoryOption> categoryOptions = new ArrayList<>( categoryService.getDataElementCategoryOptions( category ) );
            Collections.sort( categoryOptions );
            categoryOptionMap.put( category.getUid(), categoryOptions );
        }

        Collections.sort( dataSets );
        Collections.sort( categoryCombos );
        Collections.sort( categories );

        defaultCategoryCombo = categoryService.getDefaultDataElementCategoryCombo();

        return SUCCESS;
    }
}
