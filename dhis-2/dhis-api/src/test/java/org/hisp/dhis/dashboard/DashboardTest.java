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
package org.hisp.dhis.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.document.Document;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.visualization.Visualization;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class DashboardTest {

  @Test
  void testMoveItem() {
    Dashboard dashboard = new Dashboard();
    DashboardItem diA = new DashboardItem();
    DashboardItem diB = new DashboardItem();
    DashboardItem diC = new DashboardItem();
    DashboardItem diD = new DashboardItem();
    diA.setUid("A");
    diB.setUid("B");
    diC.setUid("C");
    diD.setUid("D");
    dashboard.getItems().add(diA);
    dashboard.getItems().add(diB);
    dashboard.getItems().add(diC);
    dashboard.getItems().add(diD);
    assertEquals(4, dashboard.getItems().size());
    assertEquals(2, dashboard.getItems().indexOf(diC));
    // Move B up
    assertTrue(dashboard.moveItem("B", 3));
    assertEquals(4, dashboard.getItems().size());
    assertEquals(0, dashboard.getItems().indexOf(diA));
    assertEquals(1, dashboard.getItems().indexOf(diC));
    assertEquals(2, dashboard.getItems().indexOf(diB));
    assertEquals(3, dashboard.getItems().indexOf(diD));
    // Move C last
    assertTrue(dashboard.moveItem("C", 4));
    assertEquals(0, dashboard.getItems().indexOf(diA));
    assertEquals(1, dashboard.getItems().indexOf(diB));
    assertEquals(2, dashboard.getItems().indexOf(diD));
    assertEquals(3, dashboard.getItems().indexOf(diC));
    // Move D down
    assertTrue(dashboard.moveItem("D", 1));
    assertEquals(0, dashboard.getItems().indexOf(diA));
    assertEquals(1, dashboard.getItems().indexOf(diD));
    assertEquals(2, dashboard.getItems().indexOf(diB));
    assertEquals(3, dashboard.getItems().indexOf(diC));
    // Move C first
    assertTrue(dashboard.moveItem("C", 0));
    assertEquals(0, dashboard.getItems().indexOf(diC));
    assertEquals(1, dashboard.getItems().indexOf(diA));
    assertEquals(2, dashboard.getItems().indexOf(diD));
    assertEquals(3, dashboard.getItems().indexOf(diB));
    // Out of bounds
    assertFalse(dashboard.moveItem("C", 5));
    // Already at position
    assertFalse(dashboard.moveItem("A", 1));
    // Pointless move
    assertFalse(dashboard.moveItem("A", 2));
  }

  @Test
  void testGetAvailableItemByType() {
    Dashboard dashboard = new Dashboard();
    DashboardItem diA = new DashboardItem();
    DashboardItem diB = new DashboardItem();
    DashboardItem diC = new DashboardItem();
    diA.setUid("A");
    diB.setUid("B");
    diC.setUid("C");
    diA.setVisualization(new Visualization("A"));
    diB.getReports().add(new Report("A", null, null, new Visualization()));
    diB.getReports().add(new Report("B", null, null, new Visualization()));
    diC.getResources().add(new Document("A", null, false, null));
    diC.getResources().add(new Document("B", null, false, null));
    diC.getResources().add(new Document("C", null, false, null));
    dashboard.getItems().add(diA);
    dashboard.getItems().add(diB);
    dashboard.getItems().add(diC);
    assertEquals(diB, dashboard.getAvailableItemByType(DashboardItemType.REPORTS));
    assertEquals(diC, dashboard.getAvailableItemByType(DashboardItemType.RESOURCES));
    assertNull(dashboard.getAvailableItemByType(DashboardItemType.MAP));
  }

  @Test
  void testGetItem() {
    Dashboard dashboard = new Dashboard();
    DashboardItem diA = new DashboardItem();
    DashboardItem diB = new DashboardItem();
    DashboardItem diC = new DashboardItem();
    diA.setUid("A");
    diB.setUid("B");
    diC.setUid("C");
    dashboard.getItems().add(diA);
    dashboard.getItems().add(diB);
    dashboard.getItems().add(diC);
    assertEquals(diB, dashboard.getItemByUid("B"));
    assertNull(dashboard.getItemByUid("X"));
  }
}
