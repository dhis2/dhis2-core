/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.datavalue;

import static org.hisp.dhis.scheduling.RecordingJobProgress.transitory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboGenerateService;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataInputPeriod;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.LockException;
import org.hisp.dhis.dataset.LockStatus;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the timeliness rules of data entry: when may a value for a given period be written?
 *
 * <h2>Rules under test</h2>
 *
 * Each period of an import is checked by exactly one of two mechanisms:
 *
 * <ol>
 *   <li><b>Data input periods.</b> A period that has data input periods in the data set is open
 *       only while now is inside one of its opening/closing windows. Nothing else applies to it,
 *       and no authority bypasses it; only a superuser importing with force can (see below).
 *   <li><b>Open future periods and expiry days.</b> Any other period is checked twice:
 *       <ul>
 *         <li><em>Open future periods</em> limit early entry: the period must not end after {@code
 *             PeriodType.getFuturePeriod(openFuturePeriods)}. With {@code 0} the current period is
 *             <em>not</em> open yet, with {@code 1} the current period is the latest open one, and
 *             so on. No authority bypasses this, not even F_EDIT_EXPIRED or ALL; only a superuser
 *             importing with force can (see below).
 *         <li><em>Expiry days</em> limit late entry: the period closes {@code expiryDays + 1} days
 *             after its end date (the end date is the start of the period's last day). {@code 0}
 *             means it never expires. Negative and fractional values are allowed. {@code
 *             F_EDIT_EXPIRED} (and so {@code ALL}) and lock exceptions bypass it.
 *       </ul>
 * </ol>
 *
 * Independently of both, a period must start within the operational span of the org unit (E8031). A
 * superuser importing with {@code force} skips all of these checks; {@code force} has no effect for
 * anyone else.
 *
 * <h2>Fixture</h2>
 *
 * One monthly data set with one data element, assigned to two org units A and B. Each test
 * configures the data set, then imports as one of three users, all of whom can capture data at A
 * and B:
 *
 * <pre>
 * user         | authorities
 * -------------+-----------------
 * capture      | (none)
 * editExpired  | F_EDIT_EXPIRED
 * superuser    | ALL
 * </pre>
 *
 * Periods are relative to today ({@link #month(int)}), so the tests do not depend on the date they
 * run on. Time limits are chosen with a margin of at least six hours to the real boundary.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
@Transactional
class DataEntryTimelinessValidationTest extends PostgresIntegrationTestBase {

  private static final double MILLIS_PER_DAY = (double) Duration.ofDays(1).toMillis();

  private static final PeriodType MONTHLY = new MonthlyPeriodType();

  private static final PeriodType YEARLY = new YearlyPeriodType();

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private DataEntryService dataEntryService;

  @Autowired private DataSetService dataSetService;

  @Autowired private PeriodService periodService;

  @Autowired private CategoryOptionComboGenerateService categoryOptionComboGenerateService;

  private OrganisationUnit ouA;
  private DataElement de;
  private DataSet ds;

  /** Yearly targets data set. */
  private DataSet dsTargets;

  /**
   * Each data set has its own data element, so an import targets its data set unambiguously; values
   * use the data element of their period's type.
   */
  private final Map<String, DataElement> dataElementByPeriodType = new HashMap<>();

  /** Monthly data set with an attribute combo of two categories, P and Q (one option each). */
  private DataSet dsAoc;

  private DataElement deAoc;
  private CategoryOption optionP;
  private CategoryOption optionQ;

  /** The single attribute option combo (P, Q) of {@link #dsAoc}. */
  private CategoryOptionCombo aoc;

  private User capture;
  private User editExpired;
  private User superuser;

  @BeforeEach
  void setUp() {
    Date longAgo = Date.from(Instant.EPOCH);
    ouA = createOrganisationUnit('A');
    ouA.setOpeningDate(longAgo);
    OrganisationUnit ouB = createOrganisationUnit('B');
    ouB.setOpeningDate(longAgo);
    manager.save(ouA);
    manager.save(ouB);

    de = createDataElement('X');
    manager.save(de);

    ds = createDataSet('X', MONTHLY);
    ds.addDataSetElement(de);
    ds.addOrganisationUnit(ouA);
    ds.addOrganisationUnit(ouB);
    dataSetService.addDataSet(ds);
    // addDataSet resets sharing, so open data write access afterwards
    ds.getSharing().setPublicAccess("rwrw----");
    dataSetService.updateDataSet(ds);

    dataElementByPeriodType.put(MONTHLY.getName(), de);
    dsTargets = addDataSet('T', YEARLY);

    // attribute combo of two categories, so the AOC's dates are the intersection of two options
    optionP = createCategoryOption('P');
    optionQ = createCategoryOption('Q');
    manager.save(optionP);
    manager.save(optionQ);
    // saving resets sharing, so open data write access afterwards
    optionP.getSharing().setPublicAccess("rwrw----");
    optionQ.getSharing().setPublicAccess("rwrw----");
    manager.update(optionP);
    manager.update(optionQ);
    Category categoryP = createCategory('P', optionP);
    categoryP.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    Category categoryQ = createCategory('Q', optionQ);
    categoryQ.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    manager.save(categoryP);
    manager.save(categoryQ);
    CategoryCombo comboPQ = createCategoryCombo('C', categoryP, categoryQ);
    comboPQ.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    manager.save(comboPQ);
    categoryOptionComboGenerateService.addAndPruneOptionCombos(comboPQ);
    aoc = comboPQ.getOptionCombos().iterator().next();
    deAoc = createDataElement('C');
    manager.save(deAoc);
    dsAoc = createDataSet('C', MONTHLY);
    dsAoc.setCategoryCombo(comboPQ);
    dsAoc.addDataSetElement(deAoc);
    dsAoc.addOrganisationUnit(ouA);
    dataSetService.addDataSet(dsAoc);
    dsAoc.getSharing().setPublicAccess("rwrw----");
    dataSetService.updateDataSet(dsAoc);

    Set<OrganisationUnit> both = Set.of(ouA, ouB);
    capture = createAndAddUser(false, "capture", both, both);
    // This user has the F_EDIT_EXPIRED authority, but not ALL, so it is not a superuser.
    editExpired =
        createAndAddUser(false, "editExpired", both, both, Authorities.F_EDIT_EXPIRED.name());
    superuser = createAndAddUser(true, "superuser", both, both);
  }

  // --------------------
  // Open future periods; no expiry and no data input periods.
  // --------------------

  /**
   * Boundaries of open future periods, with no expiry and no input periods. With 0 the current
   * period is not open yet; with n the latest open period is n - 1 periods after the current one.
   */
  @ParameterizedTest(name = "openFuturePeriods={0}, month({1}) open={2}")
  @CsvSource({
    "0, -1, true", // previous period is open
    "0, 0, false", // current period is not open yet
    "1, 0, true", // current period is the latest open one
    "1, 1, false", // next period is not open
    "2, 1, true", // next period is the latest open one
    "2, 2, false" // the period after next is not open
  })
  void openFuturePeriodsBoundary(int openFuturePeriods, int monthOffset, boolean open)
      throws Exception {
    configure(openFuturePeriods, 0d);
    assertTimeliness(open, capture, month(monthOffset));
  }

  /** DHIS2-22471: "Edit expired data" is about late entry and must not open future periods. */
  @Test
  void editExpiredAuthorityDoesNotOpenFuturePeriods() {
    configure(1, 0d);
    assertUntimely(editExpired, month(1));
    assertUntimely(editExpired, month(24));
  }

  /** DHIS2-22471: same with an expiry set, so the expiry branch is entered too. */
  @Test
  // Same as above, but with expiry days set. F_EDIT_EXPIRED skips the expiry check, and that must
  // not skip the open future periods check along with it (the DHIS2-22471 bug: on master both
  // checks sat inside the same F_EDIT_EXPIRED guard).
  void editExpiredAuthorityDoesNotOpenFuturePeriodsWithExpiryDays() {
    configure(1, 10d);
    assertUntimely(editExpired, month(24));
  }

  @Test
  // Future periods not enabled at all (openFuturePeriods = 0): F_EDIT_EXPIRED does not open even
  // the current period, let alone later ones.
  void editExpiredAuthorityDoesNotOpenCurrentPeriodWithZeroOpenFuturePeriods() {
    configure(0, 0d);
    assertUntimely(editExpired, month(0));
    assertUntimely(editExpired, month(1));
  }

  /** DHIS2-22471: a superuser has every authority, but still needs force for future periods. */
  @Test
  void superuserWithoutForceCannotEnterFuturePeriods() {
    configure(1, 0d);
    assertUntimely(superuser, month(24));
  }

  @Test
  // Superuser with force bypasses the open future periods check and can enter future periods.
  void superuserWithForceCanEnterFuturePeriods() throws Exception {
    configure(1, 0d);
    assertAcceptedWithForce(superuser, month(24));
  }

  @Test
  // Force has no effect for non-superusers.
  void forceHasNoEffectForNonSuperuser() {
    configure(1, 0d);
    assertUntimelyWithForce(editExpired, month(24));
    assertUntimelyWithForce(capture, month(24));
  }

  /** A negative expiry must not switch the open future periods check off. */
  @Test
  void negativeExpiryDaysDoNotDisableOpenFuturePeriods() {
    configure(1, -5d);
    assertUntimely(capture, month(24));
    assertUntimely(editExpired, month(24));
  }

  @Test
  // One import with last month and this month (both open with openFuturePeriods = 1) and 3 months
  // ahead (not open). The whole import is rejected, and the error names only the period that is
  // not open, so a user debugging the file is not sent after the valid rows.
  void untimelyErrorNamesOnlyThePeriodsNotYetOpen() {
    configure(1, 0d);
    ConflictException ex = assertUntimely(capture, month(-1), month(0), month(3));
    assertNamesPeriods(ex, month(3));
    assertDoesNotNamePeriods(ex, month(-1), month(0));
  }

  // --------------------
  // Expiry days; one open future period (so the current period is open) and no input periods.
  // --------------------

  @Test
  // Zero expiry days means the period never expires, so month(-24) is still open.
  void zeroExpiryDaysNeverExpire() throws Exception {
    configure(1, 0d);
    assertAccepted(capture, month(-24));
  }

  @Test
  // A period within the expiry days is open.
  void periodWithinExpiryDaysIsOpen() throws Exception {
    configure(1, Math.ceil(daysSinceEnd(month(-1))) + 1);
    assertAccepted(capture, month(-1));
  }

  @Test
  // A period beyond the expiry days is locked.
  void periodBeyondExpiryDaysIsLocked() {
    configure(1, Math.floor(daysSinceEnd(month(-3))) - 2);
    assertUntimely(capture, month(-3));
  }

  @Test
  // Expiry days only limit late entry: with expiry 1 the current month closes 2 days after it ends,
  // so it stays open. (It is opened by openFuturePeriods = 1, not by the expiry.) Guards against
  // the expiry check misfiring on the current period.
  void positiveExpiryDaysDoNotLockCurrentPeriod() throws Exception {
    configure(1, 1d);
    assertAccepted(capture, month(0));
  }

  @Test
  // Fractional expiry is honoured, not truncated to whole days (it was an int before #26226). The
  // expiry is chosen so month(-3) closes 6 hours from now: still open.
  void fractionalExpiryDaysJustBeforeCloseIsOpen() throws Exception {
    // closes 6 hours from now
    configure(1, daysSinceEnd(month(-3)) - 1 + 0.25);
    assertAccepted(capture, month(-3));
  }

  @Test
  // Same, but month(-3) closed 6 hours ago: locked.
  void fractionalExpiryDaysJustAfterCloseIsLocked() {
    // closed 6 hours ago
    configure(1, daysSinceEnd(month(-3)) - 1 - 0.25);
    assertUntimely(capture, month(-3));
  }

  /** Negative expiry days close a period before (or right at) its end. */
  @Test
  // Expiry -1: a period closes at the start of its own last day. Last month is therefore locked.
  // On master negative expiry was ignored entirely (only > 0 was checked).
  void negativeExpiryDaysLockPastPeriod() {
    configure(1, -1d);
    assertUntimely(capture, month(-1));
  }

  @Test
  // Negative expiry can close the CURRENT period before it ends (the targets use case: enter this
  // period's data only until some date inside it). The expiry is chosen so the current month
  // closed 2 to 3 days ago.
  void negativeExpiryDaysLockCurrentPeriodBeforeItEnds() {
    // closed 2 to 3 days ago
    configure(1, -Math.ceil(daysUntilEnd(month(0))) - 3);
    assertUntimely(capture, month(0));
  }

  @Test
  // month(-3) is past its 1-day expiry, but F_EDIT_EXPIRED may still enter it.
  void editExpiredAuthorityBypassesExpiryDays() throws Exception {
    configure(1, 1d);
    assertAccepted(editExpired, month(-3));
  }

  @Test
  // ALL implies F_EDIT_EXPIRED, so a superuser enters expired periods even without force.
  void superuserBypassesExpiryDaysWithoutForce() throws Exception {
    configure(1, 1d);
    assertAccepted(superuser, month(-3));
  }

  @Test
  // A lock exception for org unit A and month(-3) lets a normal user enter that expired period at
  // A.
  void lockExceptionOpensExpiredPeriodForItsOrgUnit() throws Exception {
    configure(1, 1d);
    addLockException(ds, ouA, month(-3));
    assertAccepted(capture, month(-3));
  }

  @Test
  // The lock exception is for org unit A only; the same period at B stays locked.
  void lockExceptionDoesNotOpenExpiredPeriodForOtherOrgUnit() {
    configure(1, 1d);
    addLockException(ds, ouA, month(-3));
    OrganisationUnit ouB = manager.get(OrganisationUnit.class, BASE_OU_UID + 'B');
    assertRejected(ErrorCode.E8030, () -> upsert(capture, false, ouB, month(-3)));
  }

  @Test
  // The lock exception is for month(-3) only; month(-4) at A stays locked.
  void lockExceptionDoesNotOpenOtherExpiredPeriod() {
    configure(1, 1d);
    addLockException(ds, ouA, month(-3));
    assertUntimely(capture, month(-4));
  }

  @Test
  // Lock exceptions only bypass expiry. A lock exception for a future period does not open it.
  void lockExceptionDoesNotOpenFuturePeriod() {
    configure(1, 1d);
    addLockException(ds, ouA, month(3));
    assertUntimely(capture, month(3));
  }

  @Test
  // Expiry chosen so last month is still open but month(-6) is not; the current month is open too.
  // The import is rejected and the error names only month(-6).
  void untimelyErrorNamesOnlyTheExpiredPeriods() {
    configure(1, Math.ceil(daysSinceEnd(month(-1))) + 1);
    ConflictException ex = assertUntimely(capture, month(-1), month(-6), month(0));
    assertNamesPeriods(ex, month(-6));
    assertDoesNotNamePeriods(ex, month(-1), month(0));
  }

  // --------------------
  // Data input periods take over from open future periods and expiry days for their periods.
  // --------------------

  /**
   * One input period for last month, with its window in days from now (empty = no date). The period
   * is open only while now is inside the window. No opening date means open from any time; no
   * closing date means open forever.
   */
  @ParameterizedTest(name = "window [{0}, {1}] days from now: open={2}")
  @CsvSource({
    "-1, 1, true", // window contains now
    "1, 5, false", // window opens tomorrow
    "-5, -1, false", // window closed yesterday
    ", 1, true", // no opening date, closes tomorrow
    "-1, , true", // opened yesterday, no closing date
    ", , true" // no dates at all: always open
  })
  void inputPeriodWindow(Integer openingDays, Integer closingDays, boolean open) throws Exception {
    configure(1, 0d);
    addInputPeriod(
        month(-1),
        openingDays == null ? null : daysFromNow(openingDays),
        closingDays == null ? null : daysFromNow(closingDays));
    assertTimeliness(open, capture, month(-1));
  }

  @Test
  // Two windows for the same period, one closed and one open: open (any window is enough).
  void anyOpenWindowOfSeveralOpensThePeriod() throws Exception {
    configure(1, 0d);
    addInputPeriod(month(-1), daysFromNow(-10), daysFromNow(-5));
    addInputPeriod(month(-1), daysFromNow(-1), daysFromNow(1));
    assertAccepted(capture, month(-1));
  }

  @Test
  // A closed window is not bypassed by F_EDIT_EXPIRED, nor by ALL without force.
  void noAuthorityBypassesClosedInputPeriod() {
    configure(1, 0d);
    addInputPeriod(month(-1), daysFromNow(-5), daysFromNow(-1));
    assertUntimely(editExpired, month(-1));
    assertUntimely(superuser, month(-1));
  }

  @Test
  // A superuser importing with force skips all timeliness checks, including a closed window.
  void superuserWithForceBypassesClosedInputPeriod() throws Exception {
    configure(1, 0d);
    addInputPeriod(month(-1), daysFromNow(-5), daysFromNow(-1));
    assertAcceptedWithForce(superuser, month(-1));
  }

  /** An open input period wins over expiry days that would have closed the period. */
  @Test
  void openInputPeriodOverridesExpiryDays() throws Exception {
    configure(1, 1d);
    addInputPeriod(month(-3), daysFromNow(-1), daysFromNow(1));
    assertAccepted(capture, month(-3));
  }

  /** An open input period wins over open future periods that would not yet open the period. */
  @Test
  void openInputPeriodOverridesOpenFuturePeriods() throws Exception {
    configure(0, 0d);
    addInputPeriod(month(3), daysFromNow(-1), daysFromNow(1));
    assertAccepted(capture, month(3));
  }

  /**
   * A period without input periods falls back to open future periods and expiry days, even when the
   * data set has input periods for other periods.
   */
  @Test
  void periodWithoutInputPeriodUsesOtherRules() throws Exception {
    configure(1, 0d);
    addInputPeriod(month(-1), daysFromNow(-5), daysFromNow(-1));
    assertAccepted(capture, month(-2));
    assertUntimely(capture, month(3));
  }

  @Test
  // Only month(-1) has an (open) input period. month(-3) has none, so it is still checked by
  // expiry days and is locked: input periods for other periods do not exempt it.
  void periodWithoutInputPeriodStillExpires() {
    configure(1, 1d);
    addInputPeriod(month(-1), daysFromNow(-1), daysFromNow(1));
    assertUntimely(capture, month(-3));
  }

  @Test
  // Mixed import: month(-1) has an open input period, month(3) has none and is not yet open
  // (openFuturePeriods = 1). The whole import is rejected and the error names only month(3).
  void mixedImportRejectsFuturePeriodWithoutInputPeriod() {
    configure(1, 0d);
    addInputPeriod(month(-1), daysFromNow(-1), daysFromNow(1));
    ConflictException ex = assertUntimely(capture, month(-1), month(3));
    assertNamesPeriods(ex, month(3));
    assertDoesNotNamePeriods(ex, month(-1));
  }

  @Test
  // Mixed import: month(-1) has a CLOSED input period, so it is locked. month(-2) has no input
  // period and is open by the other rules (no expiry). The whole import is rejected and the error
  // names only month(-1).
  void mixedImportRejectsClosedInputPeriod() {
    configure(1, 0d);
    addInputPeriod(month(-1), daysFromNow(-5), daysFromNow(-1));
    ConflictException ex = assertUntimely(capture, month(-1), month(-2));
    assertNamesPeriods(ex, month(-1));
    assertDoesNotNamePeriods(ex, month(-2));
  }

  @Test
  // month(3) is open only through its input period; month(-2) and month(0) have none and are open
  // by openFuturePeriods = 1 and no expiry. Each period uses its own rule, so all three are
  // accepted.
  void mixedImportAcceptsWhenEveryPeriodIsOpenByItsOwnRule() throws Exception {
    configure(1, 0d);
    addInputPeriod(month(3), daysFromNow(-1), daysFromNow(1));
    assertAccepted(capture, month(3), month(-2), month(0));
  }

  // --------------------
  // The org unit's opening and closed dates (E8031).
  // --------------------

  @Test
  // Org unit A opened during last month, so month(-2) starts before A existed: E8031.
  void periodBeforeOrgUnitOpeningIsRejected() {
    configure(1, 0d);
    ouA.setOpeningDate(month(-1).getEndDate());
    manager.update(ouA);
    assertRejected(ErrorCode.E8031, () -> upsert(capture, false, ouA, month(-2)));
  }

  @Test
  // Org unit A closed during month(-3), so last month starts after A closed: E8031.
  void periodAfterOrgUnitClosedIsRejected() {
    configure(1, 0d);
    ouA.setClosedDate(month(-3).getEndDate());
    manager.update(ouA);
    assertRejected(ErrorCode.E8031, () -> upsert(capture, false, ouA, month(-1)));
  }

  @Test
  // A is open from month(-3) until 30 days from now, so last month is accepted.
  void periodWhileOrgUnitIsOpenIsAccepted() throws Exception {
    configure(1, 0d);
    ouA.setOpeningDate(month(-3).getStartDate());
    ouA.setClosedDate(Date.from(daysFromNow(30)));
    manager.update(ouA);
    assertAccepted(capture, month(-1));
  }

  // --------------------
  // Yearly targets: a known use case. Targets for the current year are entered during that year,
  // but only until a cut-off inside it (for example 31 March). This is set up as
  // openFuturePeriods = 1 (the current year is open) with a negative expiryDays that closes the
  // year early. The year's end date is the start of 31 December, and a period closes
  // expiryDays + 1 days after it, so "until 31 March" (closed from the start of 1 April) is
  // expiryDays = -275 in every year (1 April to 31 December is always 274 days).
  // --------------------

  @Test
  // The real configuration: expiryDays = -275. Whether today is before or after the cut-off depends
  // on when the test runs, so the expected outcome is computed from the same cut-off.
  void yearlyTargetsWithMarch31CutOff() throws Exception {
    configure(dsTargets, 1, -275d);
    Instant cutOff = endOf(year(0)).minus(Duration.ofDays(274));
    assertTimeliness(Instant.now().isBefore(cutOff), capture, year(0));
  }

  @Test
  // Before the cut-off (here: tomorrow) the current year's targets can be entered.
  void yearlyTargetsOpenBeforeCutOff() throws Exception {
    configure(dsTargets, 1, expiryClosingAt(year(0), daysFromNow(1)));
    assertTimeliness(true, capture, year(0));
  }

  @Test
  // After the cut-off (here: yesterday) the current year is locked, although it has not ended.
  void yearlyTargetsLockedAfterCutOff() throws Exception {
    configure(dsTargets, 1, expiryClosingAt(year(0), daysFromNow(-1)));
    assertTimeliness(false, capture, year(0));
  }

  @Test
  // The cut-off is an expiry, so F_EDIT_EXPIRED may still enter targets after it.
  void yearlyTargetsAfterCutOffOpenForEditExpired() throws Exception {
    configure(dsTargets, 1, expiryClosingAt(year(0), daysFromNow(-1)));
    assertTimeliness(true, editExpired, year(0));
  }

  @Test
  // Only the current year is open: next year's targets cannot be entered yet, by anyone without
  // force, even before the cut-off. On master a negative expiry disabled this check.
  void yearlyTargetsNextYearNotOpen() throws Exception {
    configure(dsTargets, 1, expiryClosingAt(year(0), daysFromNow(1)));
    assertTimeliness(false, capture, year(1));
    assertTimeliness(false, editExpired, year(1));
    assertTimeliness(false, superuser, year(1));
  }

  @Test
  // Last year's targets are long past the cut-off.
  void yearlyTargetsLastYearLocked() throws Exception {
    configure(dsTargets, 1, expiryClosingAt(year(0), daysFromNow(1)));
    assertTimeliness(false, capture, year(-1));
  }

  @Test
  // Late submitter: after the cut-off, a lock exception reopens this year's targets for one org
  // unit.
  void yearlyTargetsLockExceptionReopensAfterCutOff() throws Exception {
    configure(dsTargets, 1, expiryClosingAt(year(0), daysFromNow(-1)));
    addLockException(dsTargets, ouA, year(0));
    assertTimeliness(true, capture, year(0));
  }

  @Test
  // A negative expiry longer than the period closes it before it starts: never open. This is a
  // misconfiguration, but the outcome must be "locked", not "open".
  void negativeExpiryLongerThanPeriodNeverOpens() throws Exception {
    configure(dsTargets, 1, -400d);
    assertTimeliness(false, capture, year(0));
  }

  // --------------------
  // Deletions go through the same timeliness rules as other data entry.
  // --------------------

  @Test
  // A value in a future period cannot be deleted by an F_EDIT_EXPIRED user (DHIS2-22471).
  void deletionInFuturePeriodIsUntimely() {
    configure(1, 0d);
    List<DataEntryValue.Input> deletion = List.of(value(de, null, month(24), true));
    assertRejected(ErrorCode.E8030, () -> upsert(editExpired, false, deletion));
  }

  @Test
  // A value in an expired period cannot be deleted by a normal user.
  void deletionInExpiredPeriodIsUntimely() {
    configure(1, 1d);
    List<DataEntryValue.Input> deletion = List.of(value(de, null, month(-3), true));
    assertRejected(ErrorCode.E8030, () -> upsert(capture, false, deletion));
  }

  @Test
  // A superuser with force can delete a value in a future period (here: one it entered with force).
  void superuserWithForceCanDeleteInFuturePeriod() throws Exception {
    configure(1, 0d);
    assertAcceptedWithForce(superuser, month(24));
    assertEquals(1, upsert(superuser, true, List.of(value(de, null, month(24), true))));
  }

  // --------------------
  // Category option start and end dates (E8032). An attribute option combo (AOC) can be used from
  // the latest start to the earliest end date of its options. "Open periods after category option
  // end date" extends the end by that many periods. As in 2.42, a period is rejected only when it
  // lies entirely outside these dates, so a period overlapping the start or end is accepted.
  // --------------------

  @Test
  // P runs from the start of month(-6) to the end of month(-2): periods inside are accepted, also
  // the first and the last one.
  void periodWithinOptionDatesIsAccepted() throws Exception {
    configureOption(optionP, month(-6).getStartDate(), month(-2).getEndDate());
    assertWithinOptionDates(true, month(-6));
    assertWithinOptionDates(true, month(-4));
    assertWithinOptionDates(true, month(-2));
  }

  @Test
  void periodBeforeOptionStartIsRejected() throws Exception {
    configureOption(optionP, month(-6).getStartDate(), month(-2).getEndDate());
    assertWithinOptionDates(false, month(-7));
  }

  @Test
  void periodAfterOptionEndIsRejected() throws Exception {
    configureOption(optionP, month(-6).getStartDate(), month(-2).getEndDate());
    assertWithinOptionDates(false, month(-1));
  }

  @Test
  // P starts in the middle of month(-6): month(-6) overlaps P, so it is accepted (2.42 rule).
  void periodOverlappingOptionStartIsAccepted() throws Exception {
    configureOption(optionP, midOf(month(-6)), month(-2).getEndDate());
    assertWithinOptionDates(true, month(-6));
  }

  @Test
  // P ends in the middle of month(-2): month(-2) overlaps P, so it is accepted (2.42 rule).
  void periodOverlappingOptionEndIsAccepted() throws Exception {
    configureOption(optionP, month(-6).getStartDate(), midOf(month(-2)));
    assertWithinOptionDates(true, month(-2));
  }

  @Test
  // P ends with month(-4); with 2 open periods after the end, month(-3) and month(-2) are still
  // accepted, month(-1) is not.
  void openPeriodsAfterOptionEndExtendTheEnd() throws Exception {
    configureOption(optionP, month(-12).getStartDate(), month(-4).getEndDate());
    configureOpenPeriodsAfterOptionEnd(2);
    assertWithinOptionDates(true, month(-3));
    assertWithinOptionDates(true, month(-2));
    assertWithinOptionDates(false, month(-1));
  }

  @Test
  // Same for an option that lasts only one period (month(-6)): the 2 extra periods are month(-5)
  // and month(-4), regardless of how long the option itself lasted.
  void openPeriodsAfterOptionEndExtendShortOption() throws Exception {
    configureOption(optionP, month(-6).getStartDate(), month(-6).getEndDate());
    configureOpenPeriodsAfterOptionEnd(2);
    assertWithinOptionDates(true, month(-5));
    assertWithinOptionDates(true, month(-4));
    assertWithinOptionDates(false, month(-3));
  }

  @Test
  // The extra periods are after the end only; they do not open periods before the start.
  void openPeriodsAfterOptionEndDoNotExtendTheStart() throws Exception {
    configureOption(optionP, month(-6).getStartDate(), month(-2).getEndDate());
    configureOpenPeriodsAfterOptionEnd(2);
    assertWithinOptionDates(false, month(-7));
  }

  @Test
  // P runs month(-6) to month(-2), Q runs month(-4) to month(0): the AOC (P, Q) can only be used
  // where both run, month(-4) to month(-2).
  void optionDatesOfAnAocIntersect() throws Exception {
    configureOption(optionP, month(-6).getStartDate(), month(-2).getEndDate());
    configureOption(optionQ, month(-4).getStartDate(), month(0).getEndDate());
    assertWithinOptionDates(false, month(-5));
    assertWithinOptionDates(true, month(-3));
    assertWithinOptionDates(false, month(-1));
  }

  @Test
  // Only an end date: no limit into the past.
  void optionWithOnlyEndDate() throws Exception {
    configureOption(optionP, null, month(-2).getEndDate());
    assertWithinOptionDates(true, month(-24));
    assertWithinOptionDates(false, month(-1));
  }

  @Test
  // Only a start date: no limit towards the present.
  void optionWithOnlyStartDate() throws Exception {
    configureOption(optionP, month(-6).getStartDate(), null);
    assertWithinOptionDates(false, month(-7));
    assertWithinOptionDates(true, month(-1));
  }

  // --------------------
  // Open future periods are counted in periods of the data set's own period type.
  // --------------------

  /**
   * For each period type: with openFuturePeriods = 0 the current period is locked and the previous
   * one open; with 1 the current period is open and the next one locked.
   */
  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        "Daily",
        "Weekly",
        "WeeklySunday",
        "BiWeekly",
        "BiMonthly",
        "Quarterly",
        "SixMonthly",
        "SixMonthlyApril",
        "FinancialApril",
        "FinancialOct"
      })
  void openFuturePeriodsForPeriodType(String periodTypeName) throws Exception {
    PeriodType type = PeriodType.getPeriodTypeByName(periodTypeName);
    DataSet dataSet = addDataSet('Z', type);
    Period current = type.createPeriod();

    configure(dataSet, 0, 0d);
    assertTimeliness(false, capture, current);
    assertTimeliness(true, capture, type.getPreviousPeriod(current));

    configure(dataSet, 1, 0d);
    assertTimeliness(true, capture, current);
    assertTimeliness(false, capture, type.getNextPeriod(current));
  }

  // --------------------
  // The lock status shown in data entry uses the same rules.
  // --------------------

  @Test
  // The lock status the data entry app shows uses the same rules as the import: a future period
  // is LOCKED for an F_EDIT_EXPIRED user (DHIS2-22471).
  void futurePeriodIsLockedForEditExpiredUser() throws Exception {
    configure(1, 0d);
    assertEquals(LockStatus.LOCKED, entryStatus(editExpired, month(24)));
  }

  @Test
  // month(-3) is past its expiry: LOCKED for a normal user.
  void expiredPeriodIsLockedForCaptureUser() throws Exception {
    configure(1, 1d);
    assertEquals(LockStatus.LOCKED, entryStatus(capture, month(-3)));
  }

  @Test
  // ... and OPEN for an F_EDIT_EXPIRED user.
  void expiredPeriodIsOpenForEditExpiredUser() throws Exception {
    configure(1, 1d);
    assertEquals(LockStatus.OPEN, entryStatus(editExpired, month(-3)));
  }

  @Test
  // An open input period shows as OPEN even though expiry and openFuturePeriods = 0 would lock
  // month(-3).
  void openInputPeriodIsOpen() throws Exception {
    configure(0, 1d);
    addInputPeriod(month(-3), daysFromNow(-1), daysFromNow(1));
    assertEquals(LockStatus.OPEN, entryStatus(capture, month(-3)));
  }

  private void configure(int openFuturePeriods, double expiryDays) {
    configure(ds, openFuturePeriods, expiryDays);
  }

  /** Changes the data set as admin (the previous import may have left a normal user logged in). */
  private void configure(DataSet dataSet, int openFuturePeriods, double expiryDays) {
    injectAdminIntoSecurityContext();
    dataSet.setOpenFuturePeriods(openFuturePeriods);
    dataSet.setExpiryDays(expiryDays);
    dataSetService.updateDataSet(dataSet);
  }

  /**
   * Adds a data set of the given period type at A with its own data element, which imports of that
   * period type then use.
   */
  private DataSet addDataSet(char uniqueCharacter, PeriodType periodType) {
    DataElement element = createDataElement(uniqueCharacter);
    manager.save(element);
    DataSet dataSet = createDataSet(uniqueCharacter, periodType);
    dataSet.addDataSetElement(element);
    dataSet.addOrganisationUnit(ouA);
    dataSetService.addDataSet(dataSet);
    dataSet.getSharing().setPublicAccess("rwrw----");
    dataSetService.updateDataSet(dataSet);
    dataElementByPeriodType.put(periodType.getName(), element);
    return dataSet;
  }

  /** Sets the start and end date of a category option (null = no date). */
  private void configureOption(CategoryOption option, Date start, Date end) {
    option.setStartDate(start);
    option.setEndDate(end);
    manager.update(option);
  }

  /** Sets how many periods after the end of a category option data can still be entered. */
  private void configureOpenPeriodsAfterOptionEnd(int periods) {
    dsAoc.setOpenPeriodsAfterCoEndDate(periods);
    dataSetService.updateDataSet(dsAoc);
  }

  /** Imports a value for the (P, Q) AOC and expects it written, or rejected with E8032. */
  private void assertWithinOptionDates(boolean valid, Period period) throws Exception {
    List<DataEntryValue.Input> values = List.of(value(deAoc, aoc, period, false));
    if (valid) assertEquals(1, upsert(capture, false, values));
    else assertRejected(ErrorCode.E8032, () -> upsert(capture, false, values));
  }

  /** A date 14 days into the period: a category option date in the middle of the period. */
  private static Date midOf(Period period) {
    return Date.from(
        Instant.ofEpochMilli(period.getStartDate().getTime()).plus(Duration.ofDays(14)));
  }

  private void addInputPeriod(Period period, Instant opening, Instant closing) {
    DataInputPeriod dip = new DataInputPeriod();
    dip.setPeriod(persisted(period));
    dip.setOpeningDate(opening == null ? null : Date.from(opening));
    dip.setClosingDate(closing == null ? null : Date.from(closing));
    ds.addDataInputPeriod(dip);
    dataSetService.updateDataSet(ds);
  }

  private void addLockException(DataSet dataSet, OrganisationUnit ou, Period period) {
    dataSetService.addLockException(new LockException(persisted(period), ou, dataSet));
  }

  /** Load-or-create; avoids getPeriod, whose ISO to ID cache outlives the per-test DB wipe. */
  private Period persisted(Period period) {
    periodService.addPeriod(period);
    return period;
  }

  /** The yearly period {@code offset} years from the current one (0 = current year). */
  private static Period year(int offset) {
    return YEARLY.getShiftedPeriod(YEARLY.createPeriod(), offset);
  }

  /** The period's end date: the start of its last day. */
  private static Instant endOf(Period period) {
    return Instant.ofEpochMilli(period.getEndDate().getTime());
  }

  /** The expiryDays that make the period close exactly at the given instant. */
  private static double expiryClosingAt(Period period, Instant cutOff) {
    return Duration.between(endOf(period), cutOff).toMillis() / MILLIS_PER_DAY - 1;
  }

  /** The monthly period {@code offset} months from the current one (0 = current month). */
  private static Period month(int offset) {
    return MONTHLY.getShiftedPeriod(MONTHLY.createPeriod(), offset);
  }

  private static Instant daysFromNow(int days) {
    return Instant.now().plus(Duration.ofDays(days));
  }

  /** Days from the period's end date (start of its last day) until now. */
  private static double daysSinceEnd(Period period) {
    return Duration.between(endOf(period), Instant.now()).toMillis() / MILLIS_PER_DAY;
  }

  /** Days from now until the period's end date (start of its last day). */
  private static double daysUntilEnd(Period period) {
    return -daysSinceEnd(period);
  }

  /** Imports one value per period at A, without force, and expects all to be written. */
  private void assertAccepted(User user, Period... periods) throws Exception {
    assertEquals(periods.length, upsert(user, false, ouA, periods));
  }

  /** Same as {@link #assertAccepted(User, Period...)}, but importing with force. */
  private void assertAcceptedWithForce(User user, Period... periods) throws Exception {
    assertEquals(periods.length, upsert(user, true, ouA, periods));
  }

  /** Calls {@link #assertAccepted} if open, else {@link #assertUntimely}. */
  private void assertTimeliness(boolean open, User user, Period period) throws Exception {
    if (open) assertAccepted(user, period);
    else assertUntimely(user, period);
  }

  /** Imports one value per period at A, without force, and expects E8030 (untimely). */
  private ConflictException assertUntimely(User user, Period... periods) {
    return assertRejected(ErrorCode.E8030, () -> upsert(user, false, ouA, periods));
  }

  /** Same as {@link #assertUntimely(User, Period...)}, but importing with force. */
  private ConflictException assertUntimelyWithForce(User user, Period... periods) {
    return assertRejected(ErrorCode.E8030, () -> upsert(user, true, ouA, periods));
  }

  /** The import is rejected as a whole with the given error code. */
  private static ConflictException assertRejected(ErrorCode code, Executable importCall) {
    ConflictException ex = assertThrows(ConflictException.class, importCall);
    assertEquals(code, ex.getCode(), ex.getMessage());
    return ex;
  }

  /** The conflict message names every one of the given periods. */
  private static void assertNamesPeriods(ConflictException ex, Period... periods) {
    String msg = ex.getMessage();
    for (Period p : periods) assertTrue(msg.contains(p.getIsoDate()), msg);
  }

  /** The conflict message names none of the given periods. */
  private static void assertDoesNotNamePeriods(ConflictException ex, Period... periods) {
    String msg = ex.getMessage();
    for (Period p : periods) assertFalse(msg.contains(p.getIsoDate()), msg);
  }

  private int upsert(User user, boolean force, OrganisationUnit ou, Period... periods)
      throws Exception {
    List<DataEntryValue.Input> values =
        Arrays.stream(periods)
            .map(p -> value(dataElementByPeriodType.get(p.getPeriodType().getName()), ou, p))
            .toList();
    return upsert(user, force, values);
  }

  private int upsert(User user, boolean force, List<DataEntryValue.Input> values) throws Exception {
    injectSecurityContextUser(user);
    return dataEntryService
        .upsertGroup(
            new DataEntryGroup.Options(false, false, force),
            dataEntryService.decodeGroup(new DataEntryGroup.Input(values)),
            transitory())
        .succeeded();
  }

  private static DataEntryValue.Input value(DataElement de, OrganisationUnit ou, Period period) {
    return new DataEntryValue.Input(
        de.getUid(), ou.getUid(), null, null, period.getIsoDate(), "1", null);
  }

  /** A value (or, if deleted, a deletion) for the monthly data element at A with the given AOC. */
  private DataEntryValue.Input value(
      DataElement de, CategoryOptionCombo aoc, Period period, boolean deleted) {
    return new DataEntryValue.Input(
        de.getUid(),
        ouA.getUid(),
        null,
        null,
        aoc == null ? null : aoc.getUid(),
        null,
        null,
        period.getIsoDate(),
        deleted ? null : "1",
        null,
        null,
        deleted);
  }

  private LockStatus entryStatus(User user, Period period) throws ConflictException {
    injectSecurityContextUser(user);
    DataValueKey key =
        new DataValueKey(UID.of(de.getUid()), UID.of(ouA.getUid()), null, null, period);
    return dataEntryService.getEntryStatus(UID.of(ds.getUid()), key);
  }
}
