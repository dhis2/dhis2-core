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
package org.hisp.dhis.sms.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Zubair Asghar.
 */
@ExtendWith(MockitoExtension.class)
class GatewayAdministrationServiceTest {
  private static final String BULKSMS = BulkSmsGatewayConfig.class.getName();

  private static final String CLICKATELL = ClickatellGatewayConfig.class.getName();

  private static final String GENERIC_GATEWAY = GenericHttpGatewayConfig.class.getName();

  private BulkSmsGatewayConfig bulkConfig;

  private ClickatellGatewayConfig clickatellConfig;

  private GenericHttpGatewayConfig genericHttpGatewayConfig;

  private SmsConfiguration spyConfiguration;

  // -------------------------------------------------------------------------
  // Mocking Dependencies
  // -------------------------------------------------------------------------

  @Mock private SmsConfigurationManager smsConfigurationManager;

  @Mock private PBEStringEncryptor pbeStringEncryptor;

  private DefaultGatewayAdministrationService subject;

  @BeforeEach
  public void setUp() {

    subject = new DefaultGatewayAdministrationService(smsConfigurationManager, pbeStringEncryptor);

    spyConfiguration = new SmsConfiguration();
    bulkConfig = new BulkSmsGatewayConfig();
    bulkConfig.setName(BULKSMS);
    bulkConfig.setPassword("password@123");

    clickatellConfig = new ClickatellGatewayConfig();
    clickatellConfig.setName(CLICKATELL);

    genericHttpGatewayConfig = new GenericHttpGatewayConfig();
    genericHttpGatewayConfig.setName(GENERIC_GATEWAY);
    genericHttpGatewayConfig.setContentType(ContentType.from("application/json").get());

    Mockito.lenient()
        .when(smsConfigurationManager.getSmsConfiguration())
        .thenReturn(spyConfiguration);
  }

  @Test
  void testGetByUid() {
    subject.addGateway(bulkConfig);
    String uid = subject.getDefaultGateway().getUid();

    SmsGatewayConfig gatewayConfig = subject.getByUid(uid);

    assertNotNull(gatewayConfig);
    assertEquals(bulkConfig, gatewayConfig);
  }

  @Test
  void testShouldReturnNullWhenUidIsNull() {
    assertNull(subject.getByUid(""));
    assertNull(subject.getByUid(null));
  }

  @Test
  void testSetDefaultGateway() {
    subject.addGateway(bulkConfig);
    subject.addGateway(clickatellConfig);

    assertNotEquals(clickatellConfig, subject.getDefaultGateway());

    subject.setDefaultGateway(clickatellConfig);

    assertEquals(clickatellConfig, subject.getDefaultGateway());
    assertNotEquals(bulkConfig, subject.getDefaultGateway());
  }

  @Test
  void testAddGateway() {
    boolean isAdded = subject.addGateway(bulkConfig);

    assertTrue(isAdded);
    assertEquals(bulkConfig, spyConfiguration.getGateways().get(0));
    assertTrue(spyConfiguration.getGateways().get(0).isDefault());

    assertTrue(subject.addGateway(genericHttpGatewayConfig));

    assertGateways(2);

    when(smsConfigurationManager.checkInstanceOfGateway(bulkConfig.getClass()))
        .thenReturn(bulkConfig);

    subject.addGateway(bulkConfig);

    // bulksms gateway already exist so it will not be added.
    assertGateways(2);
  }

  @Test
  void testUpdateDefaultGatewaySuccess() {
    assertTrue(subject.addGateway(bulkConfig));
    assertEquals(bulkConfig, subject.getDefaultGateway());
    assertEquals(BULKSMS, subject.getDefaultGateway().getName());

    BulkSmsGatewayConfig updated = new BulkSmsGatewayConfig();
    updated.setName("changedbulksms");

    subject.updateGateway(bulkConfig, updated);

    assertEquals(1, spyConfiguration.getGateways().size());
    assertEquals(updated, subject.getDefaultGateway());
    assertEquals("changedbulksms", subject.getDefaultGateway().getName());
  }

  @Test
  void testUpdateGatewaySuccess() {
    assertTrue(subject.addGateway(bulkConfig));
    assertEquals(bulkConfig, subject.getDefaultGateway());
    assertEquals(BULKSMS, subject.getDefaultGateway().getName());

    subject.addGateway(clickatellConfig);

    assertEquals(2, spyConfiguration.getGateways().size());

    ClickatellGatewayConfig updated = new ClickatellGatewayConfig();
    updated.setName(CLICKATELL);
    updated.setUid("tempUId");

    subject.updateGateway(clickatellConfig, updated);

    assertGateways(2);
    assertGateway(BULKSMS, gateway -> assertTrue(gateway.isDefault()));
    assertGateway(CLICKATELL, gateway -> assertFalse(gateway.isDefault()));
    assertGateway(CLICKATELL, gateway -> assertNotEquals("tempUId", gateway.getUid()));
  }

  @Test
  void testNothingShouldBeUpdatedIfNullIsPassed() {
    bulkConfig.setName(BULKSMS);
    assertTrue(subject.addGateway(bulkConfig));

    subject.updateGateway(bulkConfig, null);
    verify(smsConfigurationManager, timeout(0)).updateSmsConfiguration(any());
  }

  @Test
  void testWhenNoDefaultGateway() {
    assertNull(subject.getDefaultGateway());
  }

  @Test
  void testSecondGatewayIsSetToFalse() {
    when(smsConfigurationManager.getSmsConfiguration()).thenReturn(spyConfiguration);

    subject.addGateway(bulkConfig);

    clickatellConfig.setDefault(true);
    boolean isAdded = subject.addGateway(clickatellConfig);

    assertTrue(isAdded);
    assertEquals(2, spyConfiguration.getGateways().size());
    assertTrue(spyConfiguration.getGateways().contains(clickatellConfig));

    assertNotNull(subject.getDefaultGateway());
    assertEquals(subject.getDefaultGateway(), bulkConfig);
  }

  @Test
  void testRemoveDefaultGateway() {
    subject.addGateway(bulkConfig);
    subject.addGateway(clickatellConfig);

    String bulkId = getConfigByClassName(BULKSMS).getUid();
    String clickatelId = getConfigByClassName(CLICKATELL).getUid();

    assertNotNull(bulkId);
    assertNotNull(clickatelId);
    assertEquals(bulkConfig, subject.getDefaultGateway());

    assertTrue(subject.removeGatewayByUid(bulkId));
    assertGateways(1);
    assertGateway(BULKSMS, Assertions::assertNull);
    assertEquals(clickatellConfig, subject.getDefaultGateway());
  }

  @Test
  void testRemoveGateway() {
    subject.addGateway(bulkConfig);
    subject.addGateway(clickatellConfig);

    String clickatelId = getConfigByClassName(CLICKATELL).getUid();

    assertEquals(bulkConfig, subject.getDefaultGateway());
    assertGateways(2);

    assertTrue(subject.removeGatewayByUid(clickatelId));
    assertGateways(1);
    assertGateway(CLICKATELL, Assertions::assertNull);
    assertEquals(bulkConfig, subject.getDefaultGateway());
  }

  @Test
  void testRemoveSingleGateway() {
    subject.addGateway(bulkConfig);

    String bulkId = getConfigByClassName(BULKSMS).getUid();

    subject.removeGatewayByUid(bulkId);

    assertGateways(0);
    assertNull(subject.getDefaultGateway());
  }

  @Test
  void testReturnFalseIfConfigIsNull() {
    assertFalse(subject.addGateway(null));
  }

  @Test
  void testUpdateGateway() {
    assertTrue(subject.addGateway(bulkConfig));
    assertEquals(bulkConfig, subject.getDefaultGateway());
    assertEquals(BULKSMS, subject.getDefaultGateway().getName());

    BulkSmsGatewayConfig updated = bulkConfig;
    updated.setName("bulksms2");

    subject.updateGateway(bulkConfig, updated);

    assertGateways(1);
    assertGateway(BULKSMS, gateway -> assertTrue(gateway.isDefault()));
    assertGateway(BULKSMS, gateway -> assertEquals("bulksms2", gateway.getName()));
  }

  private void assertGateway(String name, Consumer<SmsGatewayConfig> test) {
    SmsGatewayConfig config = getConfigByClassName(name);
    test.accept(config);
  }

  private void assertGateways(int expectedNumberOfGateways) {
    assertEquals(expectedNumberOfGateways, spyConfiguration.getGateways().size());
  }

  private SmsGatewayConfig getConfigByClassName(String name) {
    return smsConfigurationManager.getSmsConfiguration().getGateways().stream()
        .filter(gateway -> gateway.getClass().getName().equals(name))
        .findFirst()
        .orElse(null);
  }
}
