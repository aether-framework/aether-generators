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

package de.splatgames.aether.generators.mvc.processor.utils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import de.splatgames.aether.generators.mvc.processor.struct.FieldModel;
import de.splatgames.aether.generators.mvc.processor.struct.RelKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Set;

/**
 * Static helper methods for {@code MvcBuilderProcessor}.
 * <p>
 * Provides reusable utilities for:
 * <ul>
 *   <li>String transformations (capitalize, uncapitalize, alias defaults, singularization)</li>
 *   <li>JavaPoet helpers for {@link ParameterSpec}</li>
 *   <li>Annotation and relation model checks</li>
 *   <li>Literal and default expression generation for primitives and collections</li>
 * </ul>
 * <p>
 * All methods are pure, side-effect free, and thread-safe.
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
public final class ProcessorUtils {
    private ProcessorUtils() {
    }

    // -------- naming / strings --------

    /**
     * Capitalizes the first character of the given string.
     *
     * @param s the input string, may be {@code null} or empty
     * @return the string with the first character uppercased,
     * or {@code null} if the input was {@code null}
     */
    @Nullable
    public static String capitalize(@Nullable final String s) {
        return (s == null || s.isEmpty()) ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Lowercases the first character of the given string.
     *
     * @param s the input string, may be {@code null} or empty
     * @return the string with the first character lowercased,
     * or an empty string if the input was {@code null} or empty
     */
    @NotNull
    public static String uncapitalize(@Nullable final String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Computes the default alias for a field name, handling special cases for boolean getters
     * starting with {@code isXyz}.
     *
     * @param fieldName the raw field name (never {@code null})
     * @param isBoolean whether the field type is boolean
     * @return the alias to use (never {@code null})
     */
    @NotNull
    public static String defaultAliasFor(@NotNull final String fieldName, final boolean isBoolean) {
        if (isBoolean && fieldName.startsWith("is") && fieldName.length() > 2 && Character.isUpperCase(fieldName.charAt(2))) {
            return uncapitalize(fieldName.substring(2));
        }
        return fieldName;
    }

    /**
     * Derives a singular variable name for a collection field.
     *
     * @param fieldName the original (plural) field name
     * @return a singularized variable name (never {@code null})
     */
    @NotNull
    public static String singularVar(@NotNull final String fieldName) {
        String s = GenHelpers.singularize(fieldName);
        if (s == null || s.isEmpty()) return fieldName;
        return uncapitalize(s);
    }

    // -------- javapoet param helpers --------

    /**
     * Creates a {@link ParameterSpec} for a given type and name with {@code final} modifier.
     *
     * @param t    the parameter type
     * @param name the parameter name
     * @return the parameter spec
     */
    @NotNull
    public static ParameterSpec p(@NotNull final TypeName t, @NotNull final String name) {
        return ParameterSpec.builder(t, name, Modifier.FINAL).build();
    }

    /**
     * Creates a {@link ParameterSpec} for a given class and name with {@code final} modifier.
     *
     * @param cls  the parameter class
     * @param name the parameter name
     * @return the parameter spec
     */
    @NotNull
    public static ParameterSpec p(@NotNull final Class<?> cls, @NotNull final String name) {
        return p(ClassName.get(cls), name);
    }

    // -------- model / annotations --------

    /**
     * Checks whether the given element has an annotation with the given fully-qualified name.
     *
     * @param e  the element to check
     * @param fq the fully-qualified class name of the annotation
     * @return {@code true} if present, {@code false} otherwise
     */
    public static boolean hasAnno(@NotNull final Element e, @NotNull final String fq) {
        for (AnnotationMirror am : e.getAnnotationMirrors()) {
            if (String.valueOf(am.getAnnotationType()).equals(fq)) return true;
        }
        return false;
    }

    /**
     * Resolves the relation kind for a given field based on JPA annotations.
     *
     * @param f the field element
     * @return the relation kind ({@link RelKind#TO_ONE}, {@link RelKind#TO_MANY}, or {@link RelKind#NONE})
     */
    @NotNull
    public static RelKind relationKind(@NotNull final VariableElement f) {
        if (hasAnno(f, "jakarta.persistence.ManyToOne") || hasAnno(f, "javax.persistence.ManyToOne")
                || hasAnno(f, "jakarta.persistence.OneToOne") || hasAnno(f, "javax.persistence.OneToOne")) {
            return RelKind.TO_ONE;
        }
        if (hasAnno(f, "jakarta.persistence.OneToMany") || hasAnno(f, "javax.persistence.OneToMany")
                || hasAnno(f, "jakarta.persistence.ManyToMany") || hasAnno(f, "javax.persistence.ManyToMany")) {
            return RelKind.TO_MANY;
        }
        return RelKind.NONE;
    }

    /**
     * Resolves the element type of a collection type, or {@link Object} if not parameterized.
     *
     * @param collType the collection type mirror
     * @return the element type
     */
    @NotNull
    public static TypeName resolveElementType(@NotNull final TypeMirror collType) {
        if (collType.getKind() != TypeKind.DECLARED) return TypeName.OBJECT;
        DeclaredType dt = (DeclaredType) collType;
        if (dt.getTypeArguments().isEmpty()) return TypeName.OBJECT;
        return TypeName.get(dt.getTypeArguments().get(0));
    }

    // -------- codegen literals --------

    /**
     * Returns the literal string representation of a default value for the given primitive type.
     *
     * @param t the primitive type
     * @return a string literal expression for the primitive default
     */
    @NotNull
    public static String primitiveDefaultLiteral(@NotNull final TypeName t) {
        if (t.equals(TypeName.BOOLEAN)) return "false";
        if (t.equals(TypeName.BYTE)) return "(byte)0";
        if (t.equals(TypeName.SHORT)) return "(short)0";
        if (t.equals(TypeName.INT)) return "0";
        if (t.equals(TypeName.LONG)) return "0L";
        if (t.equals(TypeName.FLOAT)) return "0f";
        if (t.equals(TypeName.DOUBLE)) return "0d";
        if (t.equals(TypeName.CHAR)) return "'\\0'";
        return "0";
    }

    /**
     * Returns the expression string for a boxed default value of the given type.
     *
     * @param t the type
     * @return a boxed default expression string (never {@code null})
     */
    @NotNull
    public static String boxedDefaultExpr(@NotNull final TypeName t) {
        if (t.equals(TypeName.BOOLEAN)) return "java.lang.Boolean.FALSE";
        if (t.equals(TypeName.BYTE)) return "java.lang.Byte.valueOf((byte)0)";
        if (t.equals(TypeName.SHORT)) return "java.lang.Short.valueOf((short)0)";
        if (t.equals(TypeName.INT)) return "java.lang.Integer.valueOf(0)";
        if (t.equals(TypeName.LONG)) return "java.lang.Long.valueOf(0L)";
        if (t.equals(TypeName.FLOAT)) return "java.lang.Float.valueOf(0f)";
        if (t.equals(TypeName.DOUBLE)) return "java.lang.Double.valueOf(0d)";
        if (t.equals(TypeName.CHAR)) return "java.lang.Character.valueOf('\\0')";
        return "null";
    }

    /**
     * Returns the default collection initialization expression for the given field model.
     *
     * @param fm the field model
     * @return code for creating a new collection ({@code ArrayList} or {@code LinkedHashSet})
     */
    @NotNull
    public static String newCollectionExpr(@NotNull final FieldModel fm) {
        return fm.isSet() ? "new java.util.LinkedHashSet<>()" : "new java.util.ArrayList<>()";
    }

    // -------- misc --------

    /**
     * Adds a method name to the given set or records it as a conflict if already present.
     *
     * @param methods   set of method names already used
     * @param conflicts list to collect conflicting names
     * @param name      the method name to add
     */
    public static void addOrConflict(@NotNull final Set<String> methods,
                                     @NotNull final List<String> conflicts,
                                     @NotNull final String name) {
        if (!methods.add(name)) conflicts.add(name);
    }
}
