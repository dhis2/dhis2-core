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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
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
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 *       and no authority bypasses it.
 *   <li><b>Open future periods and expiry days.</b> Any other period is checked twice:
 *       <ul>
 *         <li><em>Open future periods</em> limit early entry: the period must not end after {@code
 *             PeriodType.getFuturePeriod(openFuturePeriods)}. With {@code 0} the current period is
 *             <em>not</em> open yet, with {@code 1} the current period is the latest open one, and
 *             so on. No authority bypasses this, not even {@code F_EDIT_EXPIRED} or {@code ALL}.
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

  private static final double MILLIS_PER_DAY = 86_400_000d;

  private static final PeriodType MONTHLY = new MonthlyPeriodType();

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private DataEntryService dataEntryService;

  @Autowired private DataSetService dataSetService;

  @Autowired private PeriodService periodService;

  private OrganisationUnit ouA;
  private OrganisationUnit ouB;
  private DataElement de;
  private DataSet ds;

  private User capture;
  private User editExpired;
  private User superuser;

  @BeforeEach
  void setUp() {
    Date longAgo = new Date(0L);
    ouA = createOrganisationUnit('A');
    ouA.setOpeningDate(longAgo);
    ouB = createOrganisationUnit('B');
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

    Set<OrganisationUnit> both = Set.of(ouA, ouB);
    capture = createAndAddUser(false, "capture", both, both);
    editExpired =
        createAndAddUser(false, "editExpired", both, both, Authorities.F_EDIT_EXPIRED.name());
    superuser = createAndAddUser(true, "superuser", both, both);
  }

  // --------------------
  // Open future periods; no expiry and no data input periods.
  // --------------------

  @Test
  void zeroOpenFuturePeriodsLocksCurrentPeriod() {
    configure(0, 0d);
    assertUntimely(capture, month(0));
  }

  @Test
  void zeroOpenFuturePeriodsAllowsPreviousPeriod() throws Exception {
    configure(0, 0d);
    assertAccepted(capture, month(-1));
  }

  @Test
  void oneOpenFuturePeriodAllowsCurrentPeriod() throws Exception {
    configure(1, 0d);
    assertAccepted(capture, month(0));
  }

  @Test
  void oneOpenFuturePeriodLocksNextPeriod() {
    configure(1, 0d);
    assertUntimely(capture, month(1));
  }

  @Test
  void twoOpenFuturePeriodsAllowNextPeriod() throws Exception {
    configure(2, 0d);
    assertAccepted(capture, month(1));
  }

  @Test
  void twoOpenFuturePeriodsLockPeriodAfterNext() {
    configure(2, 0d);
    assertUntimely(capture, month(2));
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
  void editExpiredAuthorityDoesNotOpenFuturePeriodsWithExpiryDays() {
    configure(1, 10d);
    assertUntimely(editExpired, month(24));
  }

  /** DHIS2-22471: a superuser has every authority, but still needs force for future periods. */
  @Test
  void superuserWithoutForceCannotEnterFuturePeriods() {
    configure(1, 0d);
    assertUntimely(superuser, month(24));
  }

  @Test
  void superuserWithForceCanEnterFuturePeriods() throws Exception {
    configure(1, 0d);
    assertAccepted(superuser, true, month(24));
  }

  @Test
  void forceHasNoEffectForNonSuperuser() {
    configure(1, 0d);
    assertUntimely(editExpired, true, month(24));
    assertUntimely(capture, true, month(24));
  }

  /** A negative expiry must not switch the open future periods check off. */
  @Test
  void negativeExpiryDaysDoNotDisableOpenFuturePeriods() {
    configure(1, -5d);
    assertUntimely(capture, month(24));
    assertUntimely(editExpired, month(24));
  }

  @Test
  void reportsOnlyThePeriodsNotYetOpen() {
    configure(1, 0d);
    ConflictException ex = assertUntimely(capture, month(-1), month(0), month(3));
    assertMentionsOnly(ex, month(3), month(-1), month(0));
  }

  // --------------------
  // Expiry days; one open future period (so the current period is open) and no input periods.
  // --------------------

  @Test
  void zeroExpiryDaysNeverExpire() throws Exception {
    configure(1, 0d);
    assertAccepted(capture, month(-24));
  }

  @Test
  void periodWithinExpiryDaysIsOpen() throws Exception {
    configure(1, Math.ceil(daysSinceEnd(month(-1))) + 1);
    assertAccepted(capture, month(-1));
  }

  @Test
  void periodBeyondExpiryDaysIsLocked() {
    configure(1, Math.floor(daysSinceEnd(month(-3))) - 2);
    assertUntimely(capture, month(-3));
  }

  @Test
  void currentPeriodIsOpenWithPositiveExpiryDays() throws Exception {
    configure(1, 1d);
    assertAccepted(capture, month(0));
  }

  @Test
  void fractionalExpiryDaysJustBeforeCloseIsOpen() throws Exception {
    // closes 6 hours from now
    configure(1, daysSinceEnd(month(-3)) - 1 + 0.25);
    assertAccepted(capture, month(-3));
  }

  @Test
  void fractionalExpiryDaysJustAfterCloseIsLocked() {
    // closed 6 hours ago
    configure(1, daysSinceEnd(month(-3)) - 1 - 0.25);
    assertUntimely(capture, month(-3));
  }

  /** Negative expiry days close a period before (or right at) its end. */
  @Test
  void negativeExpiryDaysLockPastPeriod() {
    configure(1, -1d);
    assertUntimely(capture, month(-1));
  }

  @Test
  void negativeExpiryDaysLockCurrentPeriodBeforeItEnds() {
    // closes 2 days before the start of the last day of the current period
    configure(1, -Math.ceil(daysUntilEnd(month(0))) - 3);
    assertUntimely(capture, month(0));
  }

  @Test
  void editExpiredAuthorityBypassesExpiryDays() throws Exception {
    configure(1, 1d);
    assertAccepted(editExpired, month(-3));
  }

  @Test
  void superuserBypassesExpiryDaysWithoutForce() throws Exception {
    configure(1, 1d);
    assertAccepted(superuser, month(-3));
  }

  @Test
  void lockExceptionOpensExpiredPeriodForItsOrgUnit() throws Exception {
    configure(1, 1d);
    addLockException(ouA, month(-3));
    assertAccepted(capture, ouA, month(-3));
  }

  @Test
  void lockExceptionDoesNotOpenExpiredPeriodForOtherOrgUnit() {
    configure(1, 1d);
    addLockException(ouA, month(-3));
    assertUntimely(capture, ouB, month(-3));
  }

  @Test
  void lockExceptionDoesNotOpenOtherExpiredPeriod() {
    configure(1, 1d);
    addLockException(ouA, month(-3));
    assertUntimely(capture, ouA, month(-4));
  }

  @Test
  void lockExceptionDoesNotOpenFuturePeriod() {
    configure(1, 1d);
    addLockException(ouA, month(3));
    assertUntimely(capture, ouA, month(3));
  }

  @Test
  void reportsOnlyTheExpiredPeriods() {
    configure(1, Math.ceil(daysSinceEnd(month(-1))) + 1);
    ConflictException ex = assertUntimely(capture, month(-1), month(-6), month(0));
    assertMentionsOnly(ex, month(-6), month(-1), month(0));
  }

  // --------------------
  // Data input periods take over from open future periods and expiry days for their periods.
  // --------------------

  @Test
  void openInputPeriodAllowsEntry() throws Exception {
    configure(1, 0d);
    addInputPeriod(month(-1), daysFromNow(-1), daysFromNow(1));
    assertAccepted(capture, month(-1));
  }

  @Test
  void inputPeriodNotYetOpenLocksEntry() {
    configure(1, 0d);
    addInputPeriod(month(-1), daysFromNow(1), daysFromNow(5));
    assertUntimely(capture, month(-1));
  }

  @Test
  void closedInputPeriodLocksEntry() {
    configure(1, 0d);
    addInputPeriod(month(-1), daysFromNow(-5), daysFromNow(-1));
    assertUntimely(capture, month(-1));
  }

  @Test
  void inputPeriodWithoutOpeningDateIsOpenUntilClosingDate() throws Exception {
    configure(1, 0d);
    addInputPeriod(month(-1), null, daysFromNow(1));
    assertAccepted(capture, month(-1));
  }

  @Test
  void inputPeriodWithoutClosingDateIsOpenFromOpeningDate() throws Exception {
    configure(1, 0d);
    addInputPeriod(month(-1), daysFromNow(-1), null);
    assertAccepted(capture, month(-1));
  }

  @Test
  void inputPeriodWithoutAnyDatesIsAlwaysOpen() throws Exception {
    configure(1, 0d);
    addInputPeriod(month(-1), null, null);
    assertAccepted(capture, month(-1));
  }

  @Test
  void anyOpenWindowOfSeveralOpensThePeriod() throws Exception {
    configure(1, 0d);
    addInputPeriod(month(-1), daysFromNow(-10), daysFromNow(-5));
    addInputPeriod(month(-1), daysFromNow(-1), daysFromNow(1));
    assertAccepted(capture, month(-1));
  }

  @Test
  void noAuthorityBypassesClosedInputPeriod() {
    configure(1, 0d);
    addInputPeriod(month(-1), daysFromNow(-5), daysFromNow(-1));
    assertUntimely(editExpired, month(-1));
    assertUntimely(superuser, month(-1));
  }

  @Test
  void superuserWithForceBypassesClosedInputPeriod() throws Exception {
    configure(1, 0d);
    addInputPeriod(month(-1), daysFromNow(-5), daysFromNow(-1));
    assertAccepted(superuser, true, month(-1));
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
  void periodWithoutInputPeriodStillExpires() {
    configure(1, 1d);
    addInputPeriod(month(-1), daysFromNow(-1), daysFromNow(1));
    assertUntimely(capture, month(-3));
  }

  @Test
  void mixedImportRejectsFuturePeriodWithoutInputPeriod() {
    configure(1, 0d);
    addInputPeriod(month(-1), daysFromNow(-1), daysFromNow(1));
    ConflictException ex = assertUntimely(capture, month(-1), month(3));
    assertMentionsOnly(ex, month(3), month(-1));
  }

  @Test
  void mixedImportRejectsClosedInputPeriod() {
    configure(1, 0d);
    addInputPeriod(month(-1), daysFromNow(-5), daysFromNow(-1));
    ConflictException ex = assertUntimely(capture, month(-1), month(-2));
    assertMentionsOnly(ex, month(-1), month(-2));
  }

  @Test
  void mixedImportAcceptsWhenEveryPeriodIsOpenByItsOwnRule() throws Exception {
    configure(1, 0d);
    addInputPeriod(month(3), daysFromNow(-1), daysFromNow(1));
    assertAccepted(capture, month(3), month(-2), month(0));
  }

  // --------------------
  // The org unit's opening and closed dates (E8031).
  // --------------------

  @Test
  void periodBeforeOrgUnitOpeningIsRejected() {
    configure(1, 0d);
    ouA.setOpeningDate(month(-1).getEndDate());
    manager.update(ouA);
    ConflictException ex =
        assertThrows(ConflictException.class, () -> upsert(capture, false, ouA, month(-2)));
    assertEquals(ErrorCode.E8031, ex.getCode());
  }

  @Test
  void periodAfterOrgUnitClosedIsRejected() {
    configure(1, 0d);
    ouA.setClosedDate(month(-3).getEndDate());
    manager.update(ouA);
    ConflictException ex =
        assertThrows(ConflictException.class, () -> upsert(capture, false, ouA, month(-1)));
    assertEquals(ErrorCode.E8031, ex.getCode());
  }

  @Test
  void periodWhileOrgUnitIsOpenIsAccepted() throws Exception {
    configure(1, 0d);
    ouA.setOpeningDate(month(-3).getStartDate());
    ouA.setClosedDate(daysFromNow(30));
    manager.update(ouA);
    assertAccepted(capture, ouA, month(-1));
  }

  // --------------------
  // The lock status shown in data entry uses the same rules.
  // --------------------

  @Test
  void futurePeriodIsLockedForEditExpiredUser() throws Exception {
    configure(1, 0d);
    assertEquals(LockStatus.LOCKED, entryStatus(editExpired, month(24)));
  }

  @Test
  void expiredPeriodIsLockedForCaptureUser() throws Exception {
    configure(1, 1d);
    assertEquals(LockStatus.LOCKED, entryStatus(capture, month(-3)));
  }

  @Test
  void expiredPeriodIsOpenForEditExpiredUser() throws Exception {
    configure(1, 1d);
    assertEquals(LockStatus.OPEN, entryStatus(editExpired, month(-3)));
  }

  @Test
  void openInputPeriodIsOpen() throws Exception {
    configure(0, 1d);
    addInputPeriod(month(-3), daysFromNow(-1), daysFromNow(1));
    assertEquals(LockStatus.OPEN, entryStatus(capture, month(-3)));
  }

  private void configure(int openFuturePeriods, double expiryDays) {
    ds.setOpenFuturePeriods(openFuturePeriods);
    ds.setExpiryDays(expiryDays);
    dataSetService.updateDataSet(ds);
  }

  private void addInputPeriod(Period period, Date opening, Date closing) {
    DataInputPeriod dip = new DataInputPeriod();
    dip.setPeriod(persisted(period));
    dip.setOpeningDate(opening);
    dip.setClosingDate(closing);
    ds.addDataInputPeriod(dip);
    dataSetService.updateDataSet(ds);
  }

  private void addLockException(OrganisationUnit ou, Period period) {
    dataSetService.addLockException(new LockException(persisted(period), ou, ds));
  }

  /** Load-or-create; avoids getPeriod, whose ISO to ID cache outlives the per-test DB wipe. */
  private Period persisted(Period period) {
    periodService.addPeriod(period);
    return period;
  }

  /** The monthly period {@code offset} months from the current one (0 = current month). */
  private static Period month(int offset) {
    return MONTHLY.getShiftedPeriod(MONTHLY.createPeriod(), offset);
  }

  private static Date daysFromNow(double days) {
    return new Date(System.currentTimeMillis() + Math.round(days * MILLIS_PER_DAY));
  }

  /** Days from the period's end date (start of its last day) until now. */
  private static double daysSinceEnd(Period period) {
    return (System.currentTimeMillis() - period.getEndDate().getTime()) / MILLIS_PER_DAY;
  }

  /** Days from now until the period's end date (start of its last day). */
  private static double daysUntilEnd(Period period) {
    return -daysSinceEnd(period);
  }

  private void assertAccepted(User user, Period... periods) throws Exception {
    assertAccepted(user, false, periods);
  }

  private void assertAccepted(User user, boolean force, Period... periods) throws Exception {
    assertEquals(periods.length, upsert(user, force, ouA, periods));
  }

  private void assertAccepted(User user, OrganisationUnit ou, Period... periods) throws Exception {
    assertEquals(periods.length, upsert(user, false, ou, periods));
  }

  private ConflictException assertUntimely(User user, Period... periods) {
    return assertUntimely(user, false, periods);
  }

  private ConflictException assertUntimely(User user, boolean force, Period... periods) {
    return assertUntimely(user, force, ouA, periods);
  }

  private ConflictException assertUntimely(User user, OrganisationUnit ou, Period... periods) {
    return assertUntimely(user, false, ou, periods);
  }

  private ConflictException assertUntimely(
      User user, boolean force, OrganisationUnit ou, Period... periods) {
    ConflictException ex =
        assertThrows(ConflictException.class, () -> upsert(user, force, ou, periods));
    assertEquals(ErrorCode.E8030, ex.getCode(), ex.getMessage());
    return ex;
  }

  /** The conflict names the first period and none of the others. */
  private static void assertMentionsOnly(ConflictException ex, Period rejected, Period... others) {
    String msg = ex.getMessage();
    assertTrue(msg.contains(rejected.getIsoDate()), msg);
    for (Period p : others) assertFalse(msg.contains(p.getIsoDate()), msg);
  }

  private int upsert(User user, boolean force, OrganisationUnit ou, Period... periods)
      throws Exception {
    injectSecurityContextUser(user);
    List<DataEntryValue.Input> values =
        Arrays.stream(periods)
            .map(
                p ->
                    new DataEntryValue.Input(
                        de.getUid(), ou.getUid(), null, null, p.getIsoDate(), "1", null))
            .toList();
    return dataEntryService
        .upsertGroup(
            new DataEntryGroup.Options(false, false, force),
            dataEntryService.decodeGroup(new DataEntryGroup.Input(values)),
            transitory())
        .succeeded();
  }

  private LockStatus entryStatus(User user, Period period) throws ConflictException {
    injectSecurityContextUser(user);
    DataValueKey key =
        new DataValueKey(UID.of(de.getUid()), UID.of(ouA.getUid()), null, null, period);
    return dataEntryService.getEntryStatus(UID.of(ds.getUid()), key);
  }
}
