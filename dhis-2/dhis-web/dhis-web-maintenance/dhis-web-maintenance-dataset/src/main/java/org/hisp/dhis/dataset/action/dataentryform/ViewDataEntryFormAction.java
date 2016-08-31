package org.hisp.dhis.dataset.action.dataentryform;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.UserSettingService;

import com.opensymphony.xwork2.Action;

/**
 * @author Bharath Kumar
 * @version $Id$
 */
public class ViewDataEntryFormAction
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

    private UserSettingService userSettingService;

    public void setUserSettingService( UserSettingService userSettingService )
    {
        this.userSettingService = userSettingService;
    }
    
    private SystemSettingManager systemSettingManager;

    public void setSystemSettingManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }

    private DataEntryFormService dataEntryFormService;

    public void setDataEntryFormService( DataEntryFormService dataEntryFormService )
    {
        this.dataEntryFormService = dataEntryFormService;
    }
    
    private DataElementCategoryService categoryService;
    
    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private I18n i18n;

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    private int dataSetId;

    public void setDataSetId( int dataSetId )
    {
        this.dataSetId = dataSetId;
    }

    private DataEntryForm dataEntryForm;

    public DataEntryForm getDataEntryForm()
    {
        return dataEntryForm;
    }

    private DataSet dataSet;

    public DataSet getDataSet()
    {
        return dataSet;
    }

    private boolean autoSave;

    public boolean getAutoSave()
    {
        return autoSave;
    }

    private String dataEntryValue;

    public String getDataEntryValue()
    {
        return dataEntryValue;
    }

    private List<DataElement> dataElementList;

    public List<DataElement> getDataElementList()
    {
        return dataElementList;
    }

    private List<DataElementCategoryCombo> categoryCombos;

    public List<DataElementCategoryCombo> getCategoryCombos()
    {
        return categoryCombos;
    }
    
    private List<String> flags;

    public List<String> getFlags()
    {
        return flags;
    }

    // -------------------------------------------------------------------------
    // Execute
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        dataSet = dataSetService.getDataSet( dataSetId );

        dataEntryForm = dataSet.getDataEntryForm();

        dataEntryValue = dataEntryForm != null ? dataEntryFormService.prepareDataEntryFormForEdit( dataEntryForm, dataSet, i18n ) : "";

        autoSave = (Boolean) userSettingService.getUserSetting( UserSettingService.AUTO_SAVE_DATA_ENTRY_FORM, false );
        
        dataElementList = new ArrayList<>( dataSet.getDataElements() );

        Collections.sort( dataElementList, IdentifiableObjectNameComparator.INSTANCE );

        categoryCombos = new ArrayList<>( categoryService.getAllDataElementCategoryCombos() );
        
        Collections.sort( categoryCombos, IdentifiableObjectNameComparator.INSTANCE );
        
        flags = systemSettingManager.getFlags();
        
        return SUCCESS;
    }
}
