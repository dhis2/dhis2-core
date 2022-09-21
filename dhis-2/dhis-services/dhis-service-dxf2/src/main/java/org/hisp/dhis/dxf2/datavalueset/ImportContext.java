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
package org.hisp.dhis.dxf2.datavalueset;

import static java.util.Collections.emptySet;

import java.util.Date;
import java.util.Set;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportConflictDescriptor;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.callable.IdentifiableObjectCallable;
import org.hisp.dhis.user.User;
import org.hisp.quick.BatchHandler;

/**
 * All the state that needs to be tracked during a {@link DataValueSet} import.
 *
 * @author Jan Bernitt
 */
@Getter
@Builder
@AllArgsConstructor( access = AccessLevel.PRIVATE )
public final class ImportContext
{

    /*
     * Read-only state
     */

    private final ImportOptions importOptions;

    private final User currentUser;

    private final Set<OrganisationUnit> currentOrgUnits;

    private final I18n i18n;

    private final IdScheme idScheme;

    private final IdScheme dataElementIdScheme;

    private final IdScheme orgUnitIdScheme;

    private final IdScheme categoryOptComboIdScheme;

    private final IdScheme dataSetIdScheme;

    private final ImportStrategy strategy;

    private final boolean isIso8601;

    private final boolean skipLockExceptionCheck;

    private final boolean skipAudit;

    private final boolean hasSkipAuditAuth;

    private final boolean dryRun;

    private final boolean skipExistingCheck;

    private final boolean strictPeriods;

    private final boolean strictDataElements;

    private final boolean strictCategoryOptionCombos;

    private final boolean strictAttrOptionCombos;

    private final boolean strictOrgUnits;

    private final boolean requireCategoryOptionCombo;

    private final boolean requireAttrOptionCombo;

    private final boolean forceDataInput;

    private final Date now = new Date();

    /*
     * Read-write state...
     */

    private final ImportSummary summary;

    private final CachingMap<String, DataElement> dataElementMap = new CachingMap<>();

    private final CachingMap<String, OrganisationUnit> orgUnitMap = new CachingMap<>();

    private final CachingMap<String, CategoryOptionCombo> optionComboMap = new CachingMap<>();

    private final CachingMap<String, DataSet> dataElementDataSetMap = new CachingMap<>();

    private final CachingMap<String, Period> periodMap = new CachingMap<>();

    private final CachingMap<String, Set<PeriodType>> dataElementPeriodTypesMap = new CachingMap<>();

    private final CachingMap<String, Set<CategoryOptionCombo>> dataElementCategoryOptionComboMap = new CachingMap<>();

    private final CachingMap<String, Set<CategoryOptionCombo>> dataElementAttrOptionComboMap = new CachingMap<>();

    private final CachingMap<String, Boolean> dataElementOrgUnitMap = new CachingMap<>();

    private final CachingMap<String, Boolean> dataSetLockedMap = new CachingMap<>();

    private final CachingMap<String, Period> dataElementLatestFuturePeriodMap = new CachingMap<>();

    private final CachingMap<String, Boolean> orgUnitInHierarchyMap = new CachingMap<>();

    private final CachingMap<String, DateRange> attrOptionComboDateRangeMap = new CachingMap<>();

    private final CachingMap<String, Boolean> attrOptionComboOrgUnitMap = new CachingMap<>();

    private final CachingMap<String, Set<String>> dataElementOptionsMap = new CachingMap<>();

    private final CachingMap<String, Boolean> approvalMap = new CachingMap<>();

    private final CachingMap<String, Boolean> lowestApprovalLevelMap = new CachingMap<>();

    private final CachingMap<String, Boolean> periodOpenForDataElement = new CachingMap<>();

    /*
     * Data fetching and processing
     */

    private final IdentifiableObjectCallable<DataElement> dataElementCallable;

    private final IdentifiableObjectCallable<OrganisationUnit> orgUnitCallable;

    private final IdentifiableObjectCallable<CategoryOptionCombo> categoryOptionComboCallable;

    private final IdentifiableObjectCallable<CategoryOptionCombo> attributeOptionComboCallable;

    private final IdentifiableObjectCallable<Period> periodCallable;

    private final BatchHandler<org.hisp.dhis.datavalue.DataValue> dataValueBatchHandler;

    private final BatchHandler<DataValueAudit> auditBatchHandler;

    private final Function<Class<? extends IdentifiableObject>, String> singularNameForType;

    public String getCurrentUserName()
    {
        return currentUser.getUsername();
    }

    public ImportContext error()
    {
        summary.setStatus( ImportStatus.ERROR );
        return this;
    }

    public void addConflict( String object, String value )
    {
        summary.addConflict( object, value );
    }

    public void addConflict( ImportConflictDescriptor descriptor, String... objects )
    {
        addConflict( -1, descriptor, objects );
    }

    public void addConflict( int index, ImportConflictDescriptor descriptor, String... objects )
    {
        summary.addConflict( ImportConflict.createConflict( i18n, singularNameForType, index, descriptor, objects ) );
    }

    public String getStoredBy( DataValueEntry dataValue )
    {
        return dataValue.getStoredBy() == null || dataValue.getStoredBy().trim().isEmpty()
            ? getCurrentUserName()
            : dataValue.getStoredBy();
    }

    public DataSet getApprovalDataSet( DataSetContext dataSetContext, DataValueContext valueContext )
    {
        return dataSetContext.getDataSet() != null
            ? dataSetContext.getDataSet()
            : getDataElementDataSetMap().get( valueContext.getDataElement().getUid(),
                valueContext.getDataElement()::getApprovalDataSet );
    }

    /**
     * The existing persisted objects in the context of a {@link DataValueSet}
     * import.
     *
     * @author Jan Bernitt
     */
    @Getter
    @Builder
    @AllArgsConstructor( access = AccessLevel.PRIVATE )
    public static final class DataSetContext
    {
        private final DataSet dataSet;

        private final Period outerPeriod;

        private final OrganisationUnit outerOrgUnit;

        private final CategoryOptionCombo outerAttrOptionCombo;

        private final CategoryOptionCombo fallbackCategoryOptionCombo;

        public Set<DataElement> getDataSetDataElements()
        {
            return dataSet != null ? dataSet.getDataElements() : emptySet();
        }

    }

    /**
     * Context for a single {@link org.hisp.dhis.dxf2.events.event.DataValue} of
     * a {@link DataValueSet} during the import.
     *
     * @author Jan Bernitt
     */
    @Getter
    @Builder
    @AllArgsConstructor( access = AccessLevel.PRIVATE )
    public static final class DataValueContext
    {
        private final int index;

        private final DataElement dataElement;

        private final Period period;

        private final OrganisationUnit orgUnit;

        @Setter
        private CategoryOptionCombo categoryOptionCombo;

        @Setter
        private CategoryOptionCombo attrOptionCombo;

        public org.hisp.dhis.datavalue.DataValue getActualDataValue( DataValueService dataValueService )
        {
            return dataValueService.getDataValue( getDataElement(), getPeriod(), getOrgUnit(), getCategoryOptionCombo(),
                getAttrOptionCombo() );
        }
    }
}
