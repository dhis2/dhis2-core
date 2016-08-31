package org.hisp.dhis.dataset.action;

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

import com.google.common.collect.Lists;
import com.opensymphony.xwork2.Action;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.dataapproval.DataApprovalWorkflowService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.SectionService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.legend.LegendService;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.UserGroupService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hisp.dhis.commons.util.TextUtils.equalsNullSafe;

/**
 * @author Kristian
 */
public class UpdateDataSetAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

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

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private DataApprovalWorkflowService workflowService;

    public void setWorkflowService( DataApprovalWorkflowService workflowService )
    {
        this.workflowService = workflowService;
    }

    private SectionService sectionService;

    public void setSectionService( SectionService sectionService )
    {
        this.sectionService = sectionService;
    }

    private UserGroupService userGroupService;

    public void setUserGroupService( UserGroupService userGroupService )
    {
        this.userGroupService = userGroupService;
    }

    private AttributeService attributeService;

    public void setAttributeService( AttributeService attributeService )
    {
        this.attributeService = attributeService;
    }

    private LegendService legendService;

    public void setLegendService( LegendService legendService )
    {
        this.legendService = legendService;
    }

    // -------------------------------------------------------------------------
    // Input & output
    // -------------------------------------------------------------------------

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private String shortName;

    public void setShortName( String shortName )
    {
        this.shortName = shortName;
    }

    private String code;

    public void setCode( String code )
    {
        this.code = code;
    }

    private String description;

    public void setDescription( String description )
    {
        this.description = description;
    }

    private int expiryDays;

    public void setExpiryDays( int expiryDays )
    {
        this.expiryDays = expiryDays;
    }

    private int timelyDays;

    public void setTimelyDays( int timelyDays )
    {
        this.timelyDays = timelyDays;
    }

    private int notificationRecipients;

    public void setNotificationRecipients( int notificationRecipients )
    {
        this.notificationRecipients = notificationRecipients;
    }

    private boolean notifyCompletingUser;

    public void setNotifyCompletingUser( boolean notifyCompletingUser )
    {
        this.notifyCompletingUser = notifyCompletingUser;
    }

    private Integer workflowId;

    public void setWorkflowId( Integer workflowId )
    {
        this.workflowId = workflowId;
    }

    private String frequencySelect;

    public void setFrequencySelect( String frequencySelect )
    {
        this.frequencySelect = frequencySelect;
    }

    private int dataSetId;

    public void setDataSetId( int dataSetId )
    {
        this.dataSetId = dataSetId;
    }

    private int openFuturePeriods;

    public void setOpenFuturePeriods( int openFuturePeriods )
    {
        this.openFuturePeriods = openFuturePeriods;
    }

    private boolean fieldCombinationRequired;

    public void setFieldCombinationRequired( boolean fieldCombinationRequired )
    {
        this.fieldCombinationRequired = fieldCombinationRequired;
    }

    private boolean validCompleteOnly;

    public void setValidCompleteOnly( boolean validCompleteOnly )
    {
        this.validCompleteOnly = validCompleteOnly;
    }

    private boolean noValueRequiresComment;

    public void setNoValueRequiresComment( boolean noValueRequiresComment )
    {
        this.noValueRequiresComment = noValueRequiresComment;
    }

    private boolean skipOffline;

    public void setSkipOffline( boolean skipOffline )
    {
        this.skipOffline = skipOffline;
    }

    private boolean dataElementDecoration;

    public void setDataElementDecoration( boolean dataElementDecoration )
    {
        this.dataElementDecoration = dataElementDecoration;
    }

    private boolean renderAsTabs;

    public void setRenderAsTabs( boolean renderAsTabs )
    {
        this.renderAsTabs = renderAsTabs;
    }

    private boolean renderHorizontally;

    public void setRenderHorizontally( boolean renderHorizontally )
    {
        this.renderHorizontally = renderHorizontally;
    }

    private List<String> deSelected = Lists.newArrayList();

    public void setDeSelected( List<String> deSelected )
    {
        this.deSelected = deSelected;
    }

    private List<String> inSelected = Lists.newArrayList();

    public void setInSelected( List<String> inSelected )
    {
        this.inSelected = inSelected;
    }

    private Integer categoryComboId;

    public void setCategoryComboId( Integer categoryComboId )
    {
        this.categoryComboId = categoryComboId;
    }

    private Integer selectedLegendSetId;

    public void setSelectedLegendSetId( Integer selectedLegendSetId )
    {
        this.selectedLegendSetId = selectedLegendSetId;
    }

    private List<String> jsonAttributeValues;

    public void setJsonAttributeValues( List<String> jsonAttributeValues )
    {
        this.jsonAttributeValues = jsonAttributeValues;
    }

    private boolean mobile;

    public void setMobile( boolean mobile )
    {
        this.mobile = mobile;
    }

    // -------------------------------------------------------------------------
    // Action
    // -------------------------------------------------------------------------

    @Override
    public String execute() throws Exception
    {
        Set<DataElement> dataElements = new HashSet<>();

        LegendSet legendSet = legendService.getLegendSet( selectedLegendSetId );

        for ( String id : deSelected )
        {
            dataElements.add( dataElementService.getDataElement( id ) );
        }

        Set<Indicator> indicators = new HashSet<>();

        for ( String id : inSelected )
        {
            indicators.add( indicatorService.getIndicator( id ) );
        }

        PeriodType periodType = periodService.getPeriodTypeByName( frequencySelect );

        DataSet dataSet = dataSetService.getDataSet( dataSetId );

        dataSet.setExpiryDays( expiryDays );
        dataSet.setTimelyDays( timelyDays );

        if ( !(equalsNullSafe( name, dataSet.getName() ) &&
            periodType.equals( dataSet.getPeriodType() ) &&
            expiryDays == dataSet.getExpiryDays() &&
            openFuturePeriods == dataSet.getOpenFuturePeriods() &&
            dataElements.equals( dataSet.getDataElements() ) &&
            indicators.equals( dataSet.getIndicators() ) &&
            renderAsTabs == dataSet.isRenderAsTabs()) )
        {
            dataSet.increaseVersion(); // Check if version must be updated
        }

        dataSet.setName( StringUtils.trimToNull( name ) );
        dataSet.setShortName( StringUtils.trimToNull( shortName ) );
        dataSet.setCode( StringUtils.trimToNull( code ) );
        dataSet.setDescription( StringUtils.trimToNull( description ) );
        dataSet.setPeriodType( periodService.getPeriodTypeByClass( periodType.getClass() ) );
        dataSet.updateDataElements( dataElements );
        dataSet.setIndicators( indicators );
        dataSet.setOpenFuturePeriods( openFuturePeriods );
        dataSet.setFieldCombinationRequired( fieldCombinationRequired );
        dataSet.setValidCompleteOnly( validCompleteOnly );
        dataSet.setNoValueRequiresComment( noValueRequiresComment );
        dataSet.setNotifyCompletingUser( notifyCompletingUser );
        dataSet.setMobile( mobile );
        dataSet.setSkipOffline( skipOffline );
        dataSet.setDataElementDecoration( dataElementDecoration );
        dataSet.setRenderAsTabs( renderAsTabs );
        dataSet.setRenderHorizontally( renderHorizontally );
        dataSet.setNotificationRecipients( userGroupService.getUserGroup( notificationRecipients ) );
        dataSet.setLegendSet( legendSet );

        if ( categoryComboId != null )
        {
            dataSet.setCategoryCombo( categoryService.getDataElementCategoryCombo( categoryComboId ) );
        }

        if ( workflowId != null && workflowId > 0 )
        {
            dataSet.setWorkflow( workflowService.getWorkflow( workflowId ) );
        }
        else
        {
            dataSet.setWorkflow( null );
        }

        if ( jsonAttributeValues != null )
        {
            attributeService.updateAttributeValues( dataSet, jsonAttributeValues );
        }

        dataSetService.updateDataSet( dataSet );

        // ---------------------------------------------------------------------
        // Remove data elements which are removed in data set from sections
        // ---------------------------------------------------------------------

        for ( Section section : dataSet.getSections() )
        {
            if ( section.getDataElements().retainAll( dataSet.getDataElements() ) )
            {
                sectionService.updateSection( section );
            }
        }

        return SUCCESS;
    }
}
