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
package org.hisp.dhis.gist;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.hisp.dhis.jsontree.Text;
import org.hisp.dhis.schema.annotation.Gist.Transform;

/**
 * Parsing and working with the URL-parameter {@code fields} as used in Gist and metadata API.
 *
 * <p>Fields BNF
 *
 * <pre>
 *   fields = field ( ',' field )*
 *   field = name transform* ( '[' fields ']' )?
 *   transform = marker marker? ( '(' name ( ',' name )* ')' )?
 *   marker = ':' | '~' | '@'
 *   name = 'a'-'z' | 'A'-'Z' | '_' | '-' | '*'
 * </pre>
 *
 * @author Jan Bernitt
 * @param fields
 */
public record Fields(List<Field> fields) implements Iterable<Fields.Field> {

  public static final Fields DEFAULT = new Fields(List.of());

  @Nonnull
  public static Fields of(String fields) {
    if (fields == null || fields.isEmpty()) return DEFAULT;
    List<FieldExp> res = new ArrayList<>();
    parseFields(Text.of(fields), 0, res);
    return new Fields(res.stream().flatMap(e -> e.toFields("", "")).toList());
  }

  public List<String> names() {
    return fields.stream().map(Fields.Field::name).toList();
  }

  public Fields add(Field f) {
    if (fields.isEmpty()) return new Fields(List.of(f));
    List<Field> concat = new ArrayList<>(fields.size() + 1);
    concat.addAll(fields);
    concat.add(f);
    return new Fields(List.copyOf(concat));
  }

  public int size() {
    return fields.size();
  }

  @Nonnull
  @Override
  public Iterator<Field> iterator() {
    return fields.iterator();
  }

  /**
   * @param propertyPath the path as found in the schema model
   * @param renamedPath the name (also nested) as it should be rendered in the response (if
   *     different to the {@link #propertyPath()}) or empty if same as {@link #propertyPath()}
   * @param transformation
   * @param args transformation arguments
   */
  public record Field(
      @Nonnull String propertyPath,
      @Nonnull String renamedPath,
      @Nonnull Transform transformation,
      @Nonnull List<String> args) {

    public static final String REFS_PATH = "__refs__";
    public static final String ALL_PATH = "*";
    public static final Field ALL = new Field(ALL_PATH, Transform.NONE);

    public Field(String propertyPath) {
      this(propertyPath, Transform.AUTO);
    }

    public Field(String propertyPath, Transform transformation) {
      this(propertyPath, transformation, List.of());
    }

    public Field(String propertyPath, Transform transformation, List<String> args) {
      this(propertyPath, "", transformation, args);
    }

    static Field of(String field) {
      return Fields.of(field).fields.get(0);
    }

    @JsonProperty
    public String name() {
      return renamedPath.isEmpty() ? propertyPath : renamedPath;
    }

    public Field withTransformation(@Nonnull Transform transform) {
      return withTransformation(transform, List.of());
    }

    public Field withTransformation(@Nonnull Transform transform, @Nonnull List<String> args) {
      return new Field(propertyPath, renamedPath, transform, args);
    }

    public Field withPropertyPath(String path) {
      return new Field(path, renamedPath, transformation, args);
    }

    public Field withRenamedPath(String path) {
      return new Field(propertyPath, path, transformation, args);
    }

    public Field asAttribute() {
      return new Field(propertyPath, renamedPath, Transform.ATTRIBUTE, List.of());
    }

    public boolean isAttribute() {
      return transformation == Transform.ATTRIBUTE;
    }

    public boolean isAttributeAsJson() {
      return isAttribute() && !args.isEmpty();
    }

    public boolean isAuto() {
      return transformation == Transform.AUTO;
    }

    public boolean isRefs() {
      return REFS_PATH.equals(propertyPath);
    }

    public boolean isMultiPluck() {
      return transformation == Transform.PLUCK && args.size() > 1;
    }
  }

  private record TransformExp(Text type, List<Text> args) {}

  private record FieldExp(Text name, List<TransformExp> transforms, List<FieldExp> children) {

    Stream<Field> toFields(String parentPath, String parentRenamedPath) {
      String nameStr = name.toString();
      Field f = new Field(chain(parentPath, nameStr));
      String renamedName = "";
      for (TransformExp t : transforms)
        if (t.type.contentEquals("rename")) renamedName = t.args.get(0).toString();
      if (!renamedName.isEmpty() || !parentRenamedPath.isEmpty())
        f =
            f.withRenamedPath(
                chain(
                    parentRenamedPath.isEmpty() ? parentPath : parentRenamedPath,
                    renamedName.isEmpty() ? nameStr : renamedName));
      if (transforms.isEmpty() && children.isEmpty()) return Stream.of(f);
      Field base = f;
      boolean renamed = !base.renamedPath.isEmpty();
      boolean noTransform = transforms.isEmpty() || renamed && transforms.size() == 1;
      Stream<Field> transformRes =
          transforms.stream()
              .filter(t -> !t.type.contentEquals("rename"))
              .map(
                  t ->
                      base.withTransformation(
                          Transform.of(t.type.toString()),
                          t.args.stream().map(Text::toString).toList()));
      if (children.isEmpty()) return noTransform ? Stream.of(base) : transformRes;
      Field parent = f;
      Stream<Field> childrenRes =
          children.stream().flatMap(e -> e.toFields(parent.propertyPath(), parent.renamedPath()));
      return noTransform ? childrenRes : Stream.concat(transformRes, childrenRes);
    }

    static String chain(String parent, String child) {
      return parent.isEmpty() ? child : parent + "." + child;
    }
  }

  private static int parseFields(Text fields, int offset, List<FieldExp> res) {
    int i = offset;
    int len = fields.length();
    while (i < len) {
      i = parseField(fields, i, res);
      if (i >= len) return i;
      char c = fields.charAt(i);
      if (c == ']') return i;
      if (c != ',') throw expectedCharacter(',', fields, i);
      i++; // skip ,
    }
    return i;
  }

  private static int parseField(Text fields, int offset, List<FieldExp> res) {
    int i = offset;
    int len = fields.length();
    int e = parseName(fields, i);
    if (e == i) throw expectedNameCharacter(fields, i);
    Text name = fields.subSequence(i, e);
    FieldExp f = new FieldExp(name, new ArrayList<>(0), new ArrayList<>(0));
    res.add(f);
    i = e;
    if (i >= len || fields.charAt(i) == ',') return i;
    i = parseTransforms(fields, i, f.transforms);
    return parseChildren(fields, i, f.children);
  }

  private static int parseName(Text fields, int offset) {
    int i = offset;
    int len = fields.length();
    while (i < len && isNameCharacter(fields.charAt(i))) i++;
    return i;
  }

  private static int parseTransforms(Text fields, int offset, List<TransformExp> res) {
    int len = fields.length();
    int i = offset;
    while (i < len && isTransformMarker(fields.charAt(i))) {
      char c = fields.charAt(i);
      i++; // skip 1st marker
      if (fields.charAt(i) == c) i++; // allow 2nd marker
      int e = parseName(fields, i);
      if (e == i) throw expectedNameCharacter(fields, i);
      Text name = fields.subSequence(i, e);
      TransformExp t = new TransformExp(name, new ArrayList<>(0));
      res.add(t);
      i = parseNameList(fields, e, t.args);
    }
    return i;
  }

  private static int parseNameList(Text fields, int offset, List<Text> res) {
    int i = offset;
    int len = fields.length();
    if (i < len && fields.charAt(i) == '(') {
      i++; // skip (
      do {
        int e = parseName(fields, i);
        if (e == i) throw expectedNameCharacter(fields, i);
        Text arg = fields.subSequence(i, e);
        res.add(arg);
        i = e;
        if (i >= len) return i;
        char c = fields.charAt(i);
        if (c != ')' && c != ',') throw expectedCharacter(',', fields, i);
        if (c == ',') i++; // skip ,
      } while (i < len && fields.charAt(i) != ')');
      if (i >= len) throw expectedCharacter(')', fields, i);
      i++; // skip )
    }
    return i;
  }

  private static int parseChildren(Text fields, int offset, List<FieldExp> res) {
    int len = fields.length();
    int i = offset;
    if (i >= len) return i;
    if (fields.charAt(i) != '[') return i;
    i++; // skip [
    i = parseFields(fields, i, res);
    if (i >= len || fields.charAt(i) != ']') throw expectedCharacter(']', fields, i);
    return i + 1; // skip ]
  }

  private static boolean isNameCharacter(char c) {
    return c >= 'a' && c <= 'z'
        || c >= 'A' && c <= 'Z'
        || c >= '0' && c <= '9'
        || c == '_'
        || c == '-'
        || c == '*';
  }

  private static boolean isTransformMarker(char c) {
    return c == ':' || c == '~' || c == '@';
  }

  private static IllegalArgumentException expectedNameCharacter(Text fields, int offset) {
    return new IllegalArgumentException(
        "Expected a name character at position %d but found: %s"
            .formatted(offset, butFound(fields, offset)));
  }

  private static IllegalArgumentException expectedCharacter(char ch, Text fields, int offset) {
    return new IllegalArgumentException(
        "Expected %s at position %d but found: %s".formatted(ch, offset, butFound(fields, offset)));
  }

  private static String butFound(Text fields, int offset) {
    return offset >= fields.length() ? "EOF" : "" + fields.charAt(offset);
  }
}
