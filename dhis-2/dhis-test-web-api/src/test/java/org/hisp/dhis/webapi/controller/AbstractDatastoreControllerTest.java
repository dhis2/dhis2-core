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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.utils.JavaToJson.toJson;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.utils.JavaToJson;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;

/**
 * Base class for testing the {@link DatastoreController} providing helpers to set up entries in the
 * store.
 *
 * @author Jan Bernitt
 */
abstract class AbstractDatastoreControllerTest extends DhisControllerConvenienceTest {

  /**
   * Creates a new entry with the given key and value in the given namespace.
   *
   * @param ns namespace
   * @param key key of the entry
   * @param value value of the entry, valid JSON - consider using {@link JavaToJson#toJson(Object)}
   */
  final void postEntry(String ns, String key, String value) {
    assertStatus(HttpStatus.CREATED, POST("/dataStore/" + ns + "/" + key, value));
  }

  final void postPet(String key, String name, int age, List<String> eats) {
    postEntry(
        "pets",
        key,
        toJson(
            Map.of(
                "name",
                name,
                "age",
                age,
                "cute",
                true,
                "eats",
                eats == null ? List.of() : eats.stream().map(food -> Map.of("name", food)))));
  }
}
