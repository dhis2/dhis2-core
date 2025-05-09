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
package org.hisp.dhis.query;

import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface QueryParser {
  /**
   * Parses filter expressions, need 2 or 3 components depending on operator. i.e. for null you can
   * use "name:null" which checks to see if property name is null i.e. for eq you can use
   * "name:eq:ANC" which check to see if property name is equal to ANC
   *
   * <p>The general syntax is "propertyName:operatorName:<Value to check against if needed>"
   *
   * @param objectType Class type to query for
   * @param filters List of filters to add to Query
   * @param rootJunction Root junction to use (defaults to AND)
   * @return Query instance based on Schema of klass and filters list
   * @throws QueryParserException
   */
  <T extends IdentifiableObject> Query<T> parse(
      Class<T> objectType, @Nonnull List<String> filters, Junction.Type rootJunction)
      throws QueryParserException;

  <T extends IdentifiableObject> Query<T> parse(Class<T> objectType, @Nonnull List<String> filters)
      throws QueryParserException;

  Property getProperty(Schema schema, String path) throws QueryParserException;
}
