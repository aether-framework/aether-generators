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

package de.splatgames.aether.generators.dto.processor.struct;

import com.squareup.javapoet.AnnotationSpec;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * Immutable value object that describes a single source-field to be emitted into a generated DTO.
 * <p>
 * A {@code DtoField} holds all metadata the code generator needs to produce a DTO property:
 * the original field name and type, a stable ordering key, the deterministic declaration index
 * (used as tie-breaker), a boolean flag that indicates primitive {@code boolean} semantics
 * (for {@code isX} getter naming), and a list of annotation mirrors that should be copied
 * onto the generated DTO field (excluding the {@code @Dto} marker itself).
 * </p>
 *
 * <h2>Ordering semantics</h2>
 * <ul>
 *   <li>Primary order is provided via the {@code order} value (typically taken from {@code @Dto(order)}).</li>
 *   <li>Ties are deterministically broken using {@code declIndex}, the zero-based source declaration index
 *       within the declaring class.</li>
 * </ul>
 *
 * <h2>Intended usage</h2>
 * <p>
 * Instances of this type are created by the annotation processor while scanning a source class. The processor
 * groups {@code DtoField} instances by alias (DTO group), sorts them by the rules above, and then emits the
 * fields, accessors, and constructors into the target DTO type.
 * </p>
 *
 * @author Erik Pförtner
 * @implNote This class is <em>logically immutable</em>. The {@link #annotations} list reference is accepted as-is;
 * callers are expected not to mutate the list after construction (use an unmodifiable copy at the call site
 * if necessary).
 * @see de.splatgames.aether.generators.dto.annotations.Dto
 * @see de.splatgames.aether.generators.dto.processor.DtoProcessor
 * @see de.splatgames.aether.generators.dto.processor.struct.DtoGroup
 * @see de.splatgames.aether.generators.dto.processor.utils.GenUtils
 * @since 1.1.0
 */
public final class DtoField {
    private final String name;
    private final TypeMirror type;
    private final int order;
    private final int declIndex;
    private final boolean isBooleanPrimitive;
    private final List<AnnotationSpec> annotations;

    /**
     * Creates a new field descriptor used by the DTO generator.
     *
     * @param name               the simple name of the source field (never {@code null})
     * @param type               the source field type mirror (never {@code null})
     * @param order              primary ordering key (ascending)
     * @param declIndex          zero-based declaration index in the declaring class; used as a deterministic tie-breaker
     * @param isBooleanPrimitive {@code true} if the source field is a primitive {@code boolean} (drives {@code isX} getter naming)
     * @param annotations        annotations to copy onto the generated DTO field (excluding {@code @Dto}); the list must not be mutated after construction
     */
    public DtoField(@NotNull final String name,
                    @NotNull final TypeMirror type,
                    final int order,
                    final int declIndex,
                    final boolean isBooleanPrimitive,
                    @NotNull final List<AnnotationSpec> annotations) {
        this.name = name;
        this.type = type;
        this.order = order;
        this.declIndex = declIndex;
        this.isBooleanPrimitive = isBooleanPrimitive;
        this.annotations = annotations;
    }

    /**
     * Returns the simple name of the source field.
     *
     * @return the field name (never {@code null})
     */
    @NotNull
    public String getName() {
        return this.name;
    }

    /**
     * Returns the type mirror of the source field.
     *
     * @return the field type (never {@code null})
     */
    @NotNull
    public TypeMirror getType() {
        return this.type;
    }

    /**
     * Returns the primary ordering key (ascending).
     *
     * @return the {@code order} value
     */
    public int getOrder() {
        return this.order;
    }

    /**
     * Returns the zero-based declaration index inside the declaring class.
     * This is used as a deterministic tie-breaker when {@link #getOrder()} values are equal.
     *
     * @return the declaration index
     */
    public int getDeclIndex() {
        return this.declIndex;
    }

    /**
     * Returns whether the source field is a primitive {@code boolean}.
     * If {@code true}, the generator emits an {@code isX()} getter; otherwise a {@code getX()} getter.
     *
     * @return {@code true} if primitive boolean; {@code false} otherwise
     */
    public boolean isBooleanPrimitive() {
        return this.isBooleanPrimitive;
    }

    /**
     * Returns the list of annotations to be mirrored on the generated DTO field.
     * The list excludes the {@code @Dto} marker and is expected to be treated as read-only by clients.
     *
     * @return the annotation specs (never {@code null})
     */
    @NotNull
    public List<AnnotationSpec> getAnnotations() {
        return this.annotations;
    }
}
