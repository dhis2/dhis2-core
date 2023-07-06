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
package org.hisp.dhis.common;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class representing a dimension item keyword. Main responsibility is to keep information about
 * which keywords are part of a request and return as part of metadata in response.
 *
 * @author Luciano Fiandesio
 */
public class DimensionItemKeywords {
  public class Keyword {
    private String key;

    private String uid;

    private String name;

    private String code;

    Keyword(String key, String uid, String name, String code) {
      this.key = key;
      this.uid = uid;
      this.name = name;
      this.code = code;
    }

    public String getKey() {
      return key;
    }

    public MetadataItem getMetadataItem() {
      return new MetadataItem(name, uid, code);
    }
  }

  private List<Keyword> keywords;

  public DimensionItemKeywords() {
    this.keywords = new ArrayList<>();
  }

  public DimensionItemKeywords(List<IdentifiableObject> objects) {
    this.keywords = new ArrayList<>();

    this.keywords.addAll(objects.stream().map(this::asKeyword).collect(Collectors.toList()));
  }

  public void addKeyword(IdentifiableObject object) {
    this.keywords.add(asKeyword(object));
  }

  public void addKeyword(String key, String name) {
    this.keywords.add(new Keyword(key, null, name, null));
  }

  public void addKeywords(List<? extends IdentifiableObject> objects) {
    objects.forEach(object -> this.addKeyword(object));
  }

  public Keyword getKeyword(String key) {
    return keywords.stream()
        .filter(keyword -> keyword.getKey().equals(key))
        .findFirst()
        .orElse(null);
  }

  public List<Keyword> getKeywords() {
    return ImmutableList.copyOf(keywords);
  }

  public boolean isEmpty() {
    return keywords.isEmpty();
  }

  private Keyword asKeyword(IdentifiableObject object) {
    return new Keyword(object.getUid(), object.getUid(), object.getName(), object.getCode());
  }
}
