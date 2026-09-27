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
package org.hisp.dhis.reservedvalue.hibernate;

import static org.hisp.dhis.common.Objects.TRACKEDENTITYATTRIBUTE;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.reservedvalue.ReservedValue;
import org.hisp.dhis.reservedvalue.ReservedValueStore;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.textpattern.TextPattern;
import org.hisp.dhis.textpattern.TextPatternParser;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeStore;
import org.hisp.dhis.trackedentity.TrackedEntityStore;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class HibernateReservedValueStoreTest extends SingleSetupIntegrationTestBase {

  private static final String teaUid = "tea";

  private static final String prog001 = "001";

  private static final String prog002 = "002";

  private Date futureDate;

  private static final String key = "RANDOM(###)";

  private final ReservedValue.ReservedValueBuilder reservedValue =
      ReservedValue.builder()
          .ownerObject(TRACKEDENTITYATTRIBUTE.name())
          .created(new Date())
          .ownerUid(teaUid)
          .key(key)
          .expiryDate(futureDate);

  @Autowired private ReservedValueStore reservedValueStore;

  @Autowired private OrganisationUnitStore organisationUnitStore;

  @Autowired private TrackedEntityStore trackedEntityStore;

  @Autowired private TrackedEntityAttributeStore trackedEntityAttributeStore;

  @Autowired private TrackedEntityAttributeValueStore trackedEntityAttributeValueStore;

  @Autowired private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  @Override
  protected void setUpTest() {
    Calendar future = Calendar.getInstance();
    future.add(Calendar.DATE, 10);
    futureDate = future.getTime();
  }

  @BeforeEach
  void resetBuilderState() {
    reservedValue.expiryDate(futureDate);
  }

  @Test
  void reserveValuesSingleValue() {
    saveReservedValue(reservedValue.value(prog001).build());
    int count = reservedValueStore.getCount();
    saveReservedValue(reservedValue.value(prog002).build());
    assertEquals(reservedValueStore.getCount(), count + 1);
  }

  @Test
  void reserveValuesMultipleValues() {
    saveReservedValue(reservedValue.value(prog001).build());
    int count = reservedValueStore.getCount();
    int counter = 0;
    int n = 10;
    for (int i = 0; i < n; i++) {
      saveReservedValue(
          ReservedValue.builder()
              .ownerObject(TRACKEDENTITYATTRIBUTE.name())
              .created(new Date())
              .ownerUid("FREE")
              .key("00X")
              .value(String.format("%03d", counter++))
              .expiryDate(futureDate)
              .build());
    }

    assertEquals((count + counter), reservedValueStore.getCount());
  }

  @Test
  void getIfReservedValuesReturnsReservedValue() {
    ReservedValue rv = reservedValue.value(prog001).build();
    saveReservedValue(rv);
    List<ReservedValue> res =
        reservedValueStore.getAvailableValues(
            rv, Lists.newArrayList(rv.getValue()), rv.getOwnerObject());
    assertEquals(0, res.size());
  }

  @Test
  void getAvailableValuesWhenNotReserved() {
    ReservedValue rv = reservedValue.value(prog001).build();
    saveReservedValue(rv);
    assertEquals(1, reservedValueStore.getAll().size());
    List<ReservedValue> res =
        reservedValueStore.getAvailableValues(
            rv, Lists.newArrayList(prog001, prog002), rv.getOwnerObject());
    assertEquals(1, res.size());
    assertTrue(res.stream().anyMatch(r -> r.getValue().equals(prog002)));
  }

  @Test
  void getAvailableValuesWhenAlreadyUsed() throws TextPatternParser.TextPatternParsingException {
    TrackedEntityAttributeValue teav =
        saveTrackedEntityAttributeValue(
            prog001,
            tea -> {
              TextPattern textPattern = TextPatternParser.parse(key);
              textPattern.setOwnerObject(TRACKEDENTITYATTRIBUTE);
              textPattern.setOwnerUid(tea.getUid());
              tea.setTextPattern(textPattern);
            });
    ReservedValue rv = reservedValue.value(prog001).build();
    rv.setTrackedEntityAttributeId(teav.getAttribute().getId());
    assertEquals(
        1,
        trackedEntityAttributeValueService
            .getTrackedEntityAttributeValues(teav.getTrackedEntity())
            .size());
    assertEquals(0, reservedValueStore.getAll().size());
    List<ReservedValue> res =
        reservedValueStore.getAvailableValues(
            rv, Lists.newArrayList(prog001, prog002), rv.getOwnerObject());
    assertFalse(res.stream().anyMatch(r -> r.getValue().equals(prog001)));
    assertTrue(res.stream().anyMatch(r -> r.getValue().equals(prog002)));
    assertEquals(1, res.size());
  }

  @Test
  void removeExpiredReservations() {
    Calendar pastDate = Calendar.getInstance();
    pastDate.add(Calendar.DATE, -1);
    reservedValue.expiryDate(pastDate.getTime());
    ReservedValue rv = reservedValue.value(prog001).build();
    saveReservedValue(rv);
    assertTrue(reservedValueStore.getAll().contains(rv));
    reservedValueStore.removeExpiredValues();
    assertFalse(reservedValueStore.getAll().contains(rv));
  }

  @Test
  void removeExpiredReservationsDoesNotRemoveAnythingIfNothingIsExpiredOrUsed() {
    saveReservedValue(reservedValue.value(prog001).build());
    int num = reservedValueStore.getCount();
    reservedValueStore.removeExpiredValues();
    reservedValueStore.removeUsedValues();
    assertEquals(num, reservedValueStore.getCount());
  }

  @Test
  void shouldNotBeAvailableWhenAssignedToTeavWithReservedValueRowStillPresent()
      throws TextPatternParser.TextPatternParsingException {
    ReservedValue rv = reservedValue.value(prog001).build();
    saveReservedValue(rv);

    TrackedEntityAttributeValue teav = saveTrackedEntityAttributeValue(prog001);
    rv.setTrackedEntityAttributeId(teav.getAttribute().getId());

    assertEquals(1, reservedValueStore.getCount());

    List<ReservedValue> available =
        reservedValueStore.getAvailableValues(
            rv, Lists.newArrayList(prog001, prog002), rv.getOwnerObject());
    assertFalse(available.stream().anyMatch(r -> r.getValue().equals(prog001)));
    assertTrue(available.stream().anyMatch(r -> r.getValue().equals(prog002)));
  }

  @Test
  void shouldNotAddAlreadyReservedValues() throws TextPatternParser.TextPatternParsingException {
    saveReservedValue(reservedValue.value(prog001).build());
    saveTrackedEntityAttributeValue(prog001);
    assertEquals(1, reservedValueStore.getCount());
  }

  @Test
  void shouldRemoveAlreadyUsedReservedValues()
      throws TextPatternParser.TextPatternParsingException {
    ReservedValue rv = reservedValue.value(prog001).build();
    saveReservedValue(reservedValue.value(prog001).build());

    saveTrackedEntityAttributeValue(prog001);
    dbmsManager.clearSession();
    reservedValueStore.removeUsedValues();
    assertFalse(reservedValueStore.getAll().contains(rv));
    assertEquals(0, reservedValueStore.getCount());
  }

  @Test
  void shouldPreserveOriginalCaseWhenValueIsAvailable() {
    ReservedValue rv = reservedValue.value("ABC").build();

    List<ReservedValue> res =
        reservedValueStore.getAvailableValues(rv, List.of("ABC"), rv.getOwnerObject());

    assertEquals(1, res.size());
    assertEquals("ABC", res.get(0).getValue());
  }

  @Test
  void shouldNotReturnValueAsAvailableWhenValueWithDifferentCaseAlreadyReserved() {
    ReservedValue upperCaseValue = reservedValue.value("ABC").build();
    saveReservedValue(upperCaseValue);
    ReservedValue lowerCaseValue = reservedValue.value("abc").build();

    assertIsEmpty(
        reservedValueStore.getAvailableValues(
            lowerCaseValue, List.of(lowerCaseValue.getValue()), TRACKEDENTITYATTRIBUTE.name()));
  }

  @Test
  void shouldNotReturnValueAsAvailableWhenValueWithDifferentCaseAlreadyInUse()
      throws TextPatternParser.TextPatternParsingException {
    TrackedEntityAttributeValue teav = saveTrackedEntityAttributeValue("ABC");
    ReservedValue lowerCaseValue = reservedValue.value("abc").build();
    lowerCaseValue.setTrackedEntityAttributeId(teav.getAttribute().getId());

    assertIsEmpty(
        reservedValueStore.getAvailableValues(
            lowerCaseValue, List.of(lowerCaseValue.getValue()), TRACKEDENTITYATTRIBUTE.name()));
  }

  @Test
  void shouldRemoveAlreadyUsedOrExpiredReservedValues()
      throws TextPatternParser.TextPatternParsingException {
    // expired value
    Calendar pastDate = Calendar.getInstance();
    pastDate.add(Calendar.DATE, -1);
    ReservedValue rv2 = reservedValue.value(prog002).expiryDate(pastDate.getTime()).build();
    saveReservedValue(rv2);

    // used value
    saveTrackedEntityAttributeValue(prog001);
    ReservedValue rv1 = reservedValue.value(prog001).build();
    reservedValueStore.save(rv1);
    dbmsManager.clearSession();
    reservedValueStore.removeExpiredValues();
    reservedValueStore.removeUsedValues();
    assertFalse(reservedValueStore.getAll().contains(rv1));
    assertFalse(reservedValueStore.getAll().contains(rv2));
    assertEquals(0, reservedValueStore.getCount());
  }

  /**
   * Save reserved value and clear session to persist. In the reserved value store, the save method
   * is not transactional
   *
   * @param reservedValue
   */
  private void saveReservedValue(ReservedValue reservedValue) {
    reservedValueStore.save(reservedValue);
    dbmsManager.clearSession();
  }

  private TrackedEntityAttributeValue saveTrackedEntityAttributeValue(String value)
      throws TextPatternParser.TextPatternParsingException {
    return saveTrackedEntityAttributeValue(value, tea -> {});
  }

  private TrackedEntityAttributeValue saveTrackedEntityAttributeValue(
      String value, AttributeConfigurer configureAttribute)
      throws TextPatternParser.TextPatternParsingException {
    OrganisationUnit ou = createOrganisationUnit("OU");
    organisationUnitStore.save(ou);
    TrackedEntity trackedEntity = createTrackedEntity(ou);
    trackedEntityStore.save(trackedEntity);
    TrackedEntityAttribute tea = createTrackedEntityAttribute('Y');
    configureAttribute.configure(tea);
    tea.setUid(teaUid);
    trackedEntityAttributeStore.save(tea);
    TrackedEntityAttributeValue teav = createTrackedEntityAttributeValue('Z', trackedEntity, tea);
    teav.setValue(value);
    trackedEntityAttributeValueService.addTrackedEntityAttributeValue(teav);
    return teav;
  }

  private interface AttributeConfigurer {
    void configure(TrackedEntityAttribute tea) throws TextPatternParser.TextPatternParsingException;
  }
}
