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
package org.hisp.dhis.test.e2e.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import java.util.List;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.utils.SharingUtils;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class JsonObjectBuilder {
  private final JsonObject jsonObject;

  private final Configuration jsonPathConfiguration =
      Configuration.builder()
          .jsonProvider(new GsonJsonProvider())
          .options(
              Option.ALWAYS_RETURN_LIST,
              Option.SUPPRESS_EXCEPTIONS,
              Option.DEFAULT_PATH_LEAF_TO_NULL)
          .build();

  public JsonObjectBuilder() {
    jsonObject = new JsonObject();
  }

  public JsonObjectBuilder(JsonObject jsonObject) {
    this.jsonObject = jsonObject;
  }

  public static JsonObjectBuilder jsonObject() {
    return new JsonObjectBuilder();
  }

  public static JsonObjectBuilder jsonObject(JsonObject jsonObject) {
    return new JsonObjectBuilder(jsonObject);
  }

  public JsonObjectBuilder addPropertyByJsonPath(String path, String value) {
    JsonPath.using(jsonPathConfiguration).parse(jsonObject).set(path, value);

    return this;
  }

  /**
   * Adds additional property to the path.
   *
   * @param path eg "events[0]
   * @param propertyName eg "event"
   */
  public JsonObjectBuilder addPropertyByJsonPath(String path, String propertyName, String value) {
    JsonPath.using(jsonPathConfiguration).parse(jsonObject).put(path, propertyName, value);

    return this;
  }

  public JsonObjectBuilder addObjectByJsonPath(String path, Object obj) {
    JsonPath.using(jsonPathConfiguration).parse(jsonObject).set(path, obj);

    return this;
  }

  public JsonObjectBuilder addObjectByJsonPath(String path, String key, Object obj) {
    JsonPath.using(jsonPathConfiguration).parse(jsonObject).put(path, key, obj);

    return this;
  }

  public JsonObjectBuilder addProperty(String property, String value) {
    jsonObject.addProperty(property, value);

    return this;
  }

  public JsonObjectBuilder addArray(String name, List<String> array) {
    JsonArray jsonArray = new JsonArray();
    array.forEach(jsonArray::add);

    return addArray(name, jsonArray);
  }

  public JsonObjectBuilder addArray(String name, JsonArray array) {
    jsonObject.add(name, array);

    return this;
  }

  public JsonObjectBuilder addObject(String property, JsonObjectBuilder obj) {
    jsonObject.add(property, obj.build());

    return this;
  }

  public JsonObjectBuilder addObject(String property, JsonObject obj) {
    jsonObject.add(property, obj);

    return this;
  }

  public JsonObjectBuilder addArray(String property, JsonObject... objects) {
    JsonArray array = new JsonArray();
    for (JsonObject object : objects) {
      array.add(object);
    }

    jsonObject.add(property, array);

    return this;
  }

  public JsonObjectBuilder addArrayByJsonPath(
      String path, String arrayName, JsonObject... objects) {
    JsonObject object = new JsonObjectBuilder().addArray(arrayName, objects).build();

    JsonPath.using(jsonPathConfiguration)
        .parse(jsonObject)
        .put(path, arrayName, object.getAsJsonArray(arrayName));

    return this;
  }

  public JsonObjectBuilder addOrAppendToArrayByJsonPath(
      String path, String arrayName, JsonObject... objects) {
    DocumentContext context = JsonPath.using(jsonPathConfiguration).parse(jsonObject);

    if (!context.read(path + "." + arrayName).toString().contains("null")) {
      for (JsonObject obj : objects) {
        context.add(path + "." + arrayName, obj);
      }
    } else {
      this.addArrayByJsonPath(path, arrayName, objects).build();
    }

    return this;
  }

  public JsonObjectBuilder addOrAppendToArray(String property, JsonObject... objects) {
    if (jsonObject.has(property)) {
      for (JsonObject object : objects) {
        jsonObject.getAsJsonArray(property).add(object);
      }
    } else {
      addArray(property, objects);
    }

    return this;
  }

  /** Removes an object from an array if a given property of the object has a given value. */
  public JsonObjectBuilder removeFromArray(String array, String property, JsonElement value) {
    if (jsonObject.has(array)) {
      JsonArray jsonArray = jsonObject.getAsJsonArray(array);
      for (JsonElement element : jsonArray) {
        JsonElement propertyValue = ((JsonObject) element).get(property);
        if (value.getAsString().equals(propertyValue.getAsString())) {
          jsonArray.remove(element);
        }
      }
    }

    return this;
  }

  public JsonObjectBuilder addUserGroupAccess() {
    JsonObject sharing = jsonObject.getAsJsonObject("sharing");

    if (sharing == null) {
      sharing = SharingUtils.createSharingObject("rw------");
    }

    jsonObject.add(
        "sharing", SharingUtils.addUserGroupAccess(sharing, Constants.USER_GROUP_ID, "rwrw----"));
    return this;
  }

  public JsonObject wrapIntoArray(String arrayName) {
    JsonArray array = new JsonArray();

    JsonObject newObj = new JsonObject();

    array.add(jsonObject);

    newObj.add(arrayName, array);

    return newObj;
  }

  public JsonArray wrapIntoArray() {
    JsonArray array = new JsonArray();

    array.add(jsonObject);

    return array;
  }

  public JsonObjectBuilder deleteByJsonPath(String path) {
    JsonPath.using(jsonPathConfiguration).parse(jsonObject).delete(path);
    return this;
  }

  public JsonObject build() {
    return this.jsonObject;
  }
}
