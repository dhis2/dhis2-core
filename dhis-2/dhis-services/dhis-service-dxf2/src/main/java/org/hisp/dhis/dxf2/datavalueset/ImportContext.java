package org.hisp.dhis.dxf2.datavalueset;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.user.User;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * All the state that needs to be tracked during a {@link DataValueSet} import.
 *
 * @author Jan Bernitt
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ImportContext {

	private final ImportOptions importOptions;

	private final ImportSummary summary;

	private final User currentUser;

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

	public String getCurrentUserName() {
		return currentUser.getUsername();
	}

	public ImportContext error()
	{
		summary.setStatus( ImportStatus.ERROR );
		return this;
	}

	public void addConflict( String object, String value )
	{
		summary.getConflicts().add( new ImportConflict( object, value ) );
	}

	public void addConflicts(String object, List<String> values ) {
		summary.getConflicts().addAll( values.stream()
				.map( value -> new ImportConflict( object, value ) )
				.collect( Collectors.toList() ) );
	}
}
