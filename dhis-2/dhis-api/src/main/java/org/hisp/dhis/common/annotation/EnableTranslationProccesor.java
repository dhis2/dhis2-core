/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.common.annotation;

import jakarta.persistence.Column;
import jakarta.persistence.Parameter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import org.hibernate.annotations.Type;
import org.hisp.dhis.translation.Translation;
import org.springframework.javapoet.AnnotationSpec;
import org.springframework.javapoet.FieldSpec;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeSpec;

@SupportedAnnotationTypes("AddTranslations")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class EnableTranslationProccesor extends AbstractProcessor {

  private Filer filer;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    filer = processingEnv.getFiler();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(EnableTranslation.class)) {
      if (element.getKind() == ElementKind.CLASS) {
        generateTranslationsField((TypeElement) element);
      }
    }
    return true;
  }

  private void generateTranslationsField(TypeElement classElement) {
    String className = classElement.getSimpleName().toString();
    String packageName =
        processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName().toString();

    FieldSpec translationsField =
        FieldSpec.builder(
                ParameterizedTypeName.get(Set.class, Translation.class),
                "translations",
                Modifier.PRIVATE)
            .addAnnotation(Column.class)
            .addAnnotation(
                AnnotationSpec.builder(Type.class)
                    .addMember("type", "$S", "jblTranslations")
                    .addMember(
                        "parameters",
                        "$L",
                        AnnotationSpec.builder(Parameter.class)
                            .addMember("name", "$S", "clazz")
                            .addMember("value", "$S", "org.hisp.dhis.translation.Translation")
                            .build())
                    .build())
            .initializer("new $T<>()", HashSet.class)
            .build();

    TypeSpec newClass =
        TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addField(translationsField)
            .build();

    JavaFile javaFile = JavaFile.builder(packageName, newClass).build();

    try {
      javaFile.writeTo(filer);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
