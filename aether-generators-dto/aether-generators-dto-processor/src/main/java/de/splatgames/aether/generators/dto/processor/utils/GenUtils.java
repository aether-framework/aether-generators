/*
 * Copyright (c) 2025 Splatgames.de Software and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.splatgames.aether.generators.dto.processor.utils;

import com.squareup.javapoet.AnnotationSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Small, stateless utilities used by the DTO generator.
 * <p>
 * {@code GenUtils} centralizes common reflection-like helpers for annotation mirrors,
 * element/model inspection, and a few string helpers. All methods are {@code static} and
 * side-effect free. Utilities that require access to compiler services take
 * {@link Elements} and/or {@link Types} as parameters to avoid coupling to
 * processor instance state.
 * </p>
 *
 * <h2>Design goals</h2>
 * <ul>
 *   <li><strong>Purity:</strong> methods are deterministic and do not mutate inputs.</li>
 *   <li><strong>Separation:</strong> no dependency on {@code ProcessingEnvironment} fields.</li>
 *   <li><strong>Clarity:</strong> narrow, well-documented helpers for reuse across processors.</li>
 * </ul>
 *
 * @see de.splatgames.aether.generators.dto.processor.DtoProcessor
 * @see de.splatgames.aether.generators.dto.processor.struct.DtoGroup
 * @see de.splatgames.aether.generators.dto.processor.struct.DtoField
 *
 * @since 1.1.0
 * @author Erik Pförtner
 */
public final class GenUtils {

    private GenUtils() {
        /* no instances */
    }

    /**
     * Finds an annotation mirror by its fully qualified name on the given element.
     *
     * @param element the annotated element (never {@code null})
     * @param fqn     fully qualified annotation type name (never {@code null})
     * @return the matching {@link AnnotationMirror}, or {@code null} if absent
     */
    @Nullable
    public static AnnotationMirror getAnnotation(@NotNull final Element element, @NotNull final String fqn) {
        for (AnnotationMirror am : element.getAnnotationMirrors()) {
            Element type = am.getAnnotationType().asElement();
            if (type instanceof TypeElement te && te.getQualifiedName().contentEquals(fqn)) {
                return am;
            }
        }
        return null;
    }

    /**
     * Produces {@link AnnotationSpec} copies for all annotations present on the field,
     * excluding the one with the given FQCN (e.g., the generator's own marker annotation).
     *
     * @param field   the source field (never {@code null})
     * @param skipFqn fully qualified name of the annotation to exclude (never {@code null})
     * @return a new list of {@link AnnotationSpec} instances (never {@code null})
     */
    @NotNull
    public static List<AnnotationSpec> mirrorFieldAnnotations(@NotNull final VariableElement field, @NotNull final String skipFqn) {
        List<AnnotationSpec> list = new ArrayList<>();
        for (AnnotationMirror am : field.getAnnotationMirrors()) {
            Element annTypeElt = am.getAnnotationType().asElement();
            if (annTypeElt instanceof TypeElement te) {
                if (te.getQualifiedName().contentEquals(skipFqn)) {
                    continue; // skip e.g. @Dto
                }
            }
            list.add(AnnotationSpec.get(am));
        }
        return list;
    }

    /**
     * Reads a string-valued annotation member by name.
     * <p>
     * If the member is not present in the explicit values map, returns an empty string.
     * Returned string is based on {@code toString()} of the underlying value and is not trimmed.
     * </p>
     *
     * @param am   the annotation mirror (never {@code null})
     * @param name the member name (never {@code null})
     * @return the member's string value, or {@code ""} if not set
     */
    @NotNull
    public static String getStringValue(@NotNull final AnnotationMirror am, @NotNull final String name) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> ev : am.getElementValues().entrySet()) {
            if (ev.getKey().getSimpleName().contentEquals(name)) {
                Object v = ev.getValue().getValue();
                return v == null ? "" : v.toString();
            }
        }
        return "";
    }

    /**
     * Reads an int-valued annotation member by name.
     * <p>
     * If the member is not present or cannot be parsed, returns the provided default.
     * </p>
     *
     * @param am   the annotation mirror (never {@code null})
     * @param name the member name (never {@code null})
     * @param def  default value to return if unset or unparsable
     * @return the parsed integer value or {@code def}
     */
    public static int getIntValue(@NotNull final AnnotationMirror am, @NotNull final String name, final int def) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> ev : am.getElementValues().entrySet()) {
            if (ev.getKey().getSimpleName().contentEquals(name)) {
                Object v = ev.getValue().getValue();
                if (v instanceof Integer i) {
                    return i;
                }
                try {
                    return Integer.parseInt(String.valueOf(v));
                } catch (Exception ignored) {
                }
            }
        }
        return def;
    }

    /**
     * Returns {@code true} if the erasure of {@code t} is assignable to {@code java.util.List}.
     * <p>
     * Uses {@link Types#erasure(TypeMirror)} to perform assignability checks on erasures.
     * </p>
     *
     * @param elements compiler elements utility (never {@code null})
     * @param types    compiler types utility (never {@code null})
     * @param t        the type to test (never {@code null})
     * @return {@code true} if {@code t} erases to a subtype of {@code List}, otherwise {@code false}
     */
    public static boolean isListType(@NotNull final Elements elements, @NotNull final Types types, @NotNull final TypeMirror t) {
        if (!(t instanceof DeclaredType dt)) {
            return false;
        }
        TypeElement listType = elements.getTypeElement("java.util.List");
        if (listType == null) {
            return false;
        }
        return types.isAssignable(types.erasure(dt), types.erasure(listType.asType()));
    }

    /**
     * Computes the zero-based declaration index of a field within its declaring type.
     * <p>
     * The index is based on the order of {@link ElementKind#FIELD} entries in
     * {@link TypeElement#getEnclosedElements()}. If the field is not found, returns
     * {@link Integer#MAX_VALUE} as a sentinel value.
     * </p>
     *
     * @param source the declaring type (never {@code null})
     * @param field  the field to look up (never {@code null})
     * @return the zero-based index, or {@code Integer.MAX_VALUE} if not found
     */
    public static int declarationIndexOf(@NotNull final TypeElement source, @NotNull final VariableElement field) {
        int idx = 0;
        for (Element e : source.getEnclosedElements()) {
            if (e.getKind() == ElementKind.FIELD) {
                if (e.equals(field)) {
                    return idx;
                }
                idx++;
            }
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Capitalizes the first character of the string using {@link Locale#ROOT}.
     *
     * @param s input string (nullable)
     * @return capitalized string, original value if empty, or {@code null} if input was {@code null}
     */
    @Nullable
    public static String capitalize(@Nullable final String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        if (s.length() == 1) {
            return s.toUpperCase(Locale.ROOT);
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Decapitalizes the first character of the string.
     *
     * @param s input string (may be {@code null})
     * @return decapitalized string, original value if empty or {@code null}
     */
    public static String decap(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
