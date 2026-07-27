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
package org.hisp.dhis.common.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.PropertyPath;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fieldfiltering.FieldPathTransformer;
import org.hisp.dhis.jsontree.Text;
import org.hisp.dhis.schema.annotation.Gist.Transform;

/**
 * Parsing and working with the URL-parameter {@code fields} as used in Gist and metadata API.
 *
 * <p>Fields BNF
 *
 * <pre>
 *   fields = field ( ',' field )*
 *   field = marker? name transform* ( '[' fields ']' )?
 *   transform = indicator indicator? ( '(' ( name ( (',' | ';') name )* )? ')' )?
 *   marker = ':' | '-' | '!'
 *   indicator = ':' | '~' | '@' | '|'
 *   name = 'a'-'z' | 'A'-'Z' | '_' | '-' | '*' | '.'
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
    return new Fields(res.stream().flatMap(e -> e.toFields(null, null)).toList());
  }

  /**
   * Legacy support for {@link FieldPath}.
   *
   * @param fields as supplied by the client in the URL parameter
   * @return the list of {@link FieldPath} as expected by the legacy field filtering system
   */
  public static List<FieldPath> parse(String fields) {
    if (fields == null || fields.isEmpty()) return List.of();
    List<FieldExp> res = new ArrayList<>();
    parseFields(Text.of(fields), 0, res);
    return res.stream().flatMap(e -> e.toFieldPaths(null)).distinct().toList();
  }

  public List<String> names() {
    return fields.stream().map(Fields.Field::path).map(PropertyPath::toString).toList();
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
      @Nonnull PropertyPath propertyPath,
      @CheckForNull PropertyPath renamedPath,
      @Nonnull Transform transformation,
      @Nonnull List<String> args) {

    public static Field of(String field) {
      return Fields.of(field).fields.get(0);
    }

    public static final Field REFS =
        new Field(
            PropertyPath.of("__refs__"),
            PropertyPath.of("apiEndpoints"),
            Transform.NONE,
            List.of());
    public static final Field ALL = new Field(":all", Transform.NONE);

    public Field(@Nonnull String propertyPath) {
      this(propertyPath, Transform.AUTO);
    }

    public Field(@Nonnull PropertyPath propertyPath) {
      this(propertyPath, null, Transform.AUTO, List.of());
    }

    public Field(String propertyPath, Transform transformation) {
      this(propertyPath, transformation, List.of());
    }

    public Field(String propertyPath, Transform transformation, List<String> args) {
      this(PropertyPath.of(propertyPath), null, transformation, args);
    }

    /**
     * @return the effective path to render in the output
     */
    @Nonnull
    public PropertyPath path() {
      return isRenamed() ? renamedPath : propertyPath;
    }

    public Field withTransformation(@Nonnull Transform transform) {
      return withTransformation(transform, List.of());
    }

    public Field withTransformation(@Nonnull Transform transform, @Nonnull List<String> args) {
      return new Field(propertyPath, renamedPath, transform, args);
    }

    public Field withPropertyPath(String path) {
      return withPropertyPath(PropertyPath.of(path));
    }

    public Field withPropertyPath(PropertyPath path) {
      return new Field(path, renamedPath, transformation, args);
    }

    public Field withRenamedPath(String path) {
      return withRenamedPath(PropertyPath.of(path));
    }

    public Field withRenamedPath(PropertyPath path) {
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
      return propertyPath.equals(REFS.propertyPath);
    }

    public boolean isMultiPluck() {
      return transformation == Transform.PLUCK && args.size() > 1;
    }

    public boolean isExclude() {
      return propertyPath.isExclude();
    }

    public boolean isPreset() {
      return propertyPath.isPreset();
    }

    public boolean isRenamed() {
      return renamedPath != null;
    }

    public boolean isTransformed() {
      return transformation != Transform.NONE && transformation != Transform.AUTO;
    }

    public boolean isNested() {
      return propertyPath.isNested();
    }
  }

  /**
   * A field transformation node or expression in the URL mini-language exactly as entered by the
   * client
   *
   * @param type of the transformation (as entered)
   * @param args arguments for the transformer
   */
  private record TransformExp(Text type, List<Text> args) {}

  /**
   * A field node or expression in the URL mini-language exactly as entered by the client
   *
   * @param name name (including any markers)
   * @param transforms anything using a transformation indicator
   * @param children anything within the square brackets
   */
  private record FieldExp(Text name, List<TransformExp> transforms, List<FieldExp> children) {

    Stream<FieldPath> toFieldPaths(@CheckForNull PropertyPath parentPath) {
      Text name = unifiedName();
      List<FieldPathTransformer> transformers =
          this.transforms.stream()
              .map(
                  t ->
                      new FieldPathTransformer(
                          t.type.toString(), t.args.stream().map(Text::toString).toList()))
              .toList();
      PropertyPath path = PropertyPath.concat(parentPath, name);
      FieldPath f = FieldPath.of(path).withTransformers(transformers);
      if (children.isEmpty()) return Stream.of(f);
      return Stream.concat(Stream.of(f), children.stream().flatMap(c -> c.toFieldPaths(path)));
    }

    Stream<Field> toFields(
        @CheckForNull PropertyPath parentPath, @CheckForNull PropertyPath parentRenamedPath) {
      Text name = unifiedName();
      Field f = new Field(PropertyPath.concat(parentPath, name));
      Text renamedName = null;
      for (TransformExp t : transforms)
        if (t.type.contentEquals("rename")) renamedName = t.args.get(0);
      if (renamedName != null || parentRenamedPath != null)
        f =
            f.withRenamedPath(
                PropertyPath.concat(
                    parentRenamedPath == null ? parentPath : parentRenamedPath,
                    renamedName == null ? name : renamedName));
      if (transforms.isEmpty() && children.isEmpty()) return Stream.of(f);
      Field base = f;
      boolean noTransform = transforms.isEmpty() || base.isRenamed() && transforms.size() == 1;
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

    @Nonnull
    private Text unifiedName() {
      Text name = this.name;
      if (name.length() >= 2 && isExcludeMarker(name.charAt(0)) && isPresetMarker(name.charAt(1)))
        name = name.subSequence(1, name.length()); // drop negation of preset
      return name;
    }
  }

  private static int parseFields(Text fields, int offset, List<FieldExp> res) {
    int i = offset;
    boolean nested = offset > 0;
    int len = fields.length();
    while (i < len) {
      i = parseField(fields, i, nested, res);
      if (i >= len) return i;
      char c = fields.charAt(i);
      if (isNestedClose(c)) return i;
      if (c != ',') throw expectedCharacter(',', fields, i);
      i++; // skip ,
    }
    return i;
  }

  private static int parseField(Text fields, int offset, boolean nested, List<FieldExp> res) {
    int len = fields.length();
    int i = skipSpace(fields, offset);
    if (i >= len) return i; // allow empty field at the end (ignore dangling ,)
    char c = fields.charAt(i);
    if (c == ',' || nested && isNestedClose(c)) return i; // allow empty field (ignore)
    int s = i;
    while (s < len && isNameMarker(fields.charAt(s))) s++; // skip : or * of a preset
    int e = parseName(fields, s);
    if (e == i) throw expectedNameCharacter(fields, i);
    Text name = fields.subSequence(i, e);
    FieldExp f = new FieldExp(name, new ArrayList<>(0), new ArrayList<>(0));
    res.add(f);
    i = skipSpace(fields, e);
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
      if (i < len && fields.charAt(i) == c) i++; // allow 2nd marker
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
      while (i < len && fields.charAt(i) != ')') {
        int e = parseName(fields, i);
        if (e == i) throw expectedNameCharacter(fields, i);
        Text arg = fields.subSequence(i, e);
        res.add(arg);
        i = e;
        if (i >= len) return i;
        char c = fields.charAt(i);
        if (c != ')' && !isSeparator(c)) throw expectedCharacter(',', fields, i);
        if (isSeparator(c)) i++; // skip , or ;
      }
      if (i >= len) throw expectedCharacter(')', fields, i);
      i++; // skip )
    }
    return i;
  }

  private static int parseChildren(Text fields, int offset, List<FieldExp> res) {
    int len = fields.length();
    int i = offset;
    if (i >= len) return i;
    if (!isNestedOpen(fields.charAt(i))) return i;
    i++; // skip [ or (
    i = skipSpace(fields, i);
    if (i >= len) return i;
    if (isNestedClose(fields.charAt(i))) return i + 1; // skip ] or )
    i = parseFields(fields, i, res);
    if (i >= len) return i; // allow omitting ] or ) at the end
    if (!isNestedClose(fields.charAt(i))) throw expectedCharacter(']', fields, i);
    return i + 1; // skip ] or )
  }

  private static int skipSpace(Text fields, int offset) {
    int i = offset;
    int len = fields.length();
    while (i < len && fields.charAt(i) == ' ') i++;
    return i;
  }

  private static boolean isNameCharacter(char c) {
    return c >= 'a' && c <= 'z'
        || c >= 'A' && c <= 'Z'
        || c >= '0' && c <= '9'
        || c == '_'
        || c == '-'
        || c == '.';
  }

  private static boolean isTransformMarker(char c) {
    return c == ':' || c == '~' || c == '@' || c == '|';
  }

  private static boolean isNameMarker(char c) {
    return isPresetMarker(c) || isExcludeMarker(c);
  }

  private static boolean isPresetMarker(char c) {
    return c == ':' || c == '*';
  }

  private static boolean isExcludeMarker(char c) {
    return c == '!' || c == '-';
  }

  private static boolean isNestedOpen(char c) {
    return c == '[' || c == '(';
  }

  private static boolean isNestedClose(char c) {
    return c == ']' || c == ')';
  }

  private static boolean isSeparator(char c) {
    return c == ',' || c == ';';
  }

  private static IllegalArgumentException expectedNameCharacter(Text fields, int offset) {
    return new IllegalArgumentException(
        "Expected a name character at position %d but found: %s%s"
            .formatted(offset, butFound(fields, offset), marker(fields, offset)));
  }

  private static IllegalArgumentException expectedCharacter(char ch, Text fields, int offset) {
    return new IllegalArgumentException(
        "Expected %s at position %d but found: %s%s"
            .formatted(ch, offset, butFound(fields, offset), marker(fields, offset)));
  }

  private static String marker(Text fields, int offset) {
    char[] indent = new char[offset];
    Arrays.fill(indent, ' ');
    String marker = new String(indent) + "^";
    return "%n  %s%n  %s".formatted(fields, marker);
  }

  private static String butFound(Text fields, int offset) {
    return offset >= fields.length() ? "EOI" : "" + fields.charAt(offset);
  }
}
