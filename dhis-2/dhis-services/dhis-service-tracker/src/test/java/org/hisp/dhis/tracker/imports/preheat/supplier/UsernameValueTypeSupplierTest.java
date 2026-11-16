/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsernameValueTypeSupplierTest extends TestBase {

  private static final String USERNAME_ATTRIBUTE = "hQKI6KcEu5t";

  private static final String USERNAME_DATA_ELEMENT = "kKacJUdANDC";

  private static final String NUMERIC_DATA_ELEMENT = "DiszpKrYNg8";

  private UsernameValueTypeSupplier supplier;

  private TrackerPreheat preheat;

  @Mock private UserService userService;

  @BeforeEach
  void setUp() {
    preheat = new TrackerPreheat();
    supplier = new UsernameValueTypeSupplier(userService);
  }

  @Test
  void shouldAddUserReferencedByTrackedEntityAttribute() {
    preheat.put(TrackerIdSchemeParam.UID, List.of(usernameAttribute()));
    when(userService.getUsersByUsernames(List.of("android"))).thenReturn(List.of(user("android")));

    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .trackedEntities(
                List.of(
                    TrackedEntity.builder()
                        .trackedEntity(UID.generate())
                        .attributes(List.of(attribute("android")))
                        .build()))
            .build();

    supplier.preheatAdd(trackerObjects, preheat);

    assertTrue(preheat.getUserByUsername("android").isPresent());
  }

  @Test
  void shouldAddUserReferencedByEnrollmentAttribute() {
    preheat.put(TrackerIdSchemeParam.UID, List.of(usernameAttribute()));
    when(userService.getUsersByUsernames(List.of("android"))).thenReturn(List.of(user("android")));

    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .enrollments(
                List.of(
                    Enrollment.builder()
                        .enrollment(UID.generate())
                        .attributes(List.of(attribute("android")))
                        .build()))
            .build();

    supplier.preheatAdd(trackerObjects, preheat);

    assertTrue(preheat.getUserByUsername("android").isPresent());
  }

  @Test
  void shouldAddUserReferencedByEventDataValue() {
    preheat.put(
        TrackerIdSchemeParam.UID,
        List.of(
            dataElement(USERNAME_DATA_ELEMENT, ValueType.USERNAME),
            dataElement(NUMERIC_DATA_ELEMENT, ValueType.NUMBER)));
    when(userService.getUsersByUsernames(List.of("android"))).thenReturn(List.of(user("android")));

    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .events(
                List.of(
                    event(
                        dataValue(USERNAME_DATA_ELEMENT, "android"),
                        dataValue(NUMERIC_DATA_ELEMENT, "2"))))
            .build();

    supplier.preheatAdd(trackerObjects, preheat);

    assertTrue(preheat.getUserByUsername("android").isPresent());
  }

  @Test
  void shouldNotLookUpUserIfEventDataValueIsEmpty() {
    preheat.put(
        TrackerIdSchemeParam.UID, List.of(dataElement(USERNAME_DATA_ELEMENT, ValueType.USERNAME)));
    when(userService.getUsersByUsernames(List.of())).thenReturn(List.of());

    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .events(List.of(event(dataValue(USERNAME_DATA_ELEMENT, ""))))
            .build();

    supplier.preheatAdd(trackerObjects, preheat);

    assertFalse(preheat.getUserByUsername("").isPresent());
  }

  @Test
  void shouldNotLookUpValuesOfNonUsernameDataElements() {
    preheat.put(
        TrackerIdSchemeParam.UID, List.of(dataElement(NUMERIC_DATA_ELEMENT, ValueType.TEXT)));
    when(userService.getUsersByUsernames(List.of())).thenReturn(List.of());

    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .events(List.of(event(dataValue(NUMERIC_DATA_ELEMENT, "android"))))
            .build();

    supplier.preheatAdd(trackerObjects, preheat);

    verify(userService, never()).getUsersByUsernames(List.of("android"));
    assertFalse(preheat.getUserByUsername("android").isPresent());
  }

  private TrackedEntityAttribute usernameAttribute() {
    TrackedEntityAttribute attribute = createTrackedEntityAttribute('A');
    attribute.setUid(USERNAME_ATTRIBUTE);
    attribute.setValueType(ValueType.USERNAME);
    return attribute;
  }

  private Attribute attribute(String value) {
    return Attribute.builder()
        .attribute(MetadataIdentifier.ofUid(USERNAME_ATTRIBUTE))
        .value(value)
        .build();
  }

  private DataElement dataElement(String uid, ValueType valueType) {
    DataElement dataElement = createDataElement('A');
    dataElement.setUid(uid);
    dataElement.setValueType(valueType);
    return dataElement;
  }

  private DataValue dataValue(String dataElement, String value) {
    return DataValue.builder()
        .dataElement(MetadataIdentifier.ofUid(dataElement))
        .value(value)
        .build();
  }

  private Event event(DataValue... dataValues) {
    return Event.builder().event(UID.generate()).dataValues(Set.of(dataValues)).build();
  }

  private User user(String username) {
    User user = new User();
    user.setUid(UID.generate().getValue());
    user.setUsername(username);
    return user;
  }
}
