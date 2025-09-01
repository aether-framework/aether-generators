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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Logical grouping of fields that belong to a single generated DTO type.
 * <p>
 * A {@code DtoGroup} represents one output DTO for a given source class. Groups are keyed by an
 * <em>alias</em> (typically taken from {@code @Dto(value)}); the empty alias denotes the default
 * DTO whose simple name is usually derived from the source type (e.g., {@code UserDto}). Each group
 * carries the target package and DTO simple name, a reference to the source type (simple name and FQCN),
 * and a collection of {@link DtoField} entries that will be emitted to the generated DTO.
 * </p>
 *
 * <h2>Class-level marker annotation</h2>
 * <p>
 * Some configurations attach a marker annotation to the generated DTO type (e.g., {@code lombok.Data}).
 * The {@linkplain #mergeClassAnnotation(String) merge} method ensures that all fields within the same
 * group agree on the same marker; conflicting values fail fast with an {@link IllegalStateException}.
 * </p>
 *
 * <h2>Deterministic ordering</h2>
 * <p>
 * The {@linkplain #sort() sort} method arranges fields by their primary {@code order} key (ascending),
 * and uses the zero-based source declaration index as a stable tie-breaker. This guarantees
 * deterministic output across builds.
 * </p>
 *
 * @implNote
 * The {@linkplain #getFields() field list} returned by this class is a <em>live</em> list owned by the
 * generator. It is intentionally mutable to allow the processor to accumulate and sort entries prior
 * to code emission. External consumers should treat it as implementation detail and avoid mutating it.
 *
 * @see de.splatgames.aether.generators.dto.annotations.Dto
 * @see de.splatgames.aether.generators.dto.processor.struct.DtoField
 * @see de.splatgames.aether.generators.dto.processor.DtoProcessor
 * @see de.splatgames.aether.generators.dto.processor.utils.GenUtils
 *
 * @since 1.1.0
 * @author Erik Pförtner
 */
public final class DtoGroup {
    private final String alias;
    private final String dtoSimpleName;
    private final String targetPackage;
    private final String sourceSimpleName;
    private final String sourceFqn;
    @Nullable
    private String classAnnotationFqn;
    final List<DtoField> fields = new ArrayList<>();

    /**
     * Creates a new group descriptor for a single DTO to be generated.
     *
     * @param alias          the alias that identifies this group (empty string for the default DTO; never {@code null})
     * @param dtoSimpleName  the simple name of the target DTO type (e.g., {@code UserDto}) (never {@code null})
     * @param targetPackage  the package into which the DTO will be generated (never {@code null})
     * @param sourceSimpleName the simple name of the declaring source class (never {@code null})
     * @param sourceFqn      the fully qualified name of the declaring source class (never {@code null})
     */
    public DtoGroup(@NotNull final String alias,
                    @NotNull final String dtoSimpleName,
                    @NotNull final String targetPackage,
                    @NotNull final String sourceSimpleName,
                    @NotNull final String sourceFqn) {
        this.alias = alias;
        this.dtoSimpleName = dtoSimpleName;
        this.targetPackage = targetPackage;
        this.sourceSimpleName = sourceSimpleName;
        this.sourceFqn = sourceFqn;
    }

    /**
     * Returns the live list of fields that belong to this DTO group.
     * <p>Callers may append to this list and subsequently invoke {@link #sort()}.</p>
     *
     * @return the live field list (never {@code null})
     */
    @NotNull
    public List<DtoField> getFields() {
        return this.fields;
    }

    /**
     * Returns the alias identifying this group.
     * <p>An empty string denotes the default DTO for the source type.</p>
     *
     * @return the alias (never {@code null})
     */
    @NotNull
    public String getAlias() {
        return this.alias;
    }

    /**
     * Returns the simple name of the target DTO type (e.g., {@code UserDto}).
     *
     * @return the DTO simple name (never {@code null})
     */
    @NotNull
    public String getDtoSimpleName() {
        return this.dtoSimpleName;
    }

    /**
     * Returns the target package into which the DTO will be generated.
     *
     * @return the target package (never {@code null})
     */
    @NotNull
    public String getTargetPackage() {
        return this.targetPackage;
    }

    /**
     * Returns the simple name of the declaring source class.
     *
     * @return the source simple name (never {@code null})
     */
    @NotNull
    public String getSourceSimpleName() {
        return this.sourceSimpleName;
    }

    /**
     * Returns the fully qualified name of the declaring source class.
     *
     * @return the source FQCN (never {@code null})
     */
    @NotNull
    public String getSourceFqn() {
        return this.sourceFqn;
    }

    /**
     * Returns the fully qualified name of the marker annotation to apply to the DTO class,
     * or {@code null} if none is configured.
     *
     * @return the marker annotation FQCN, or {@code null}
     */
    @Nullable
    public String getClassAnnotationFqn() {
        return this.classAnnotationFqn;
    }

    /**
     * Merges a new marker annotation FQCN into this group, enforcing consistency.
     * <p>
     * If the current group has no marker yet, {@code fqn} is recorded (may be {@code null} to indicate "no marker").
     * If a marker is already recorded and {@code fqn} differs, an exception is thrown to prevent ambiguous output.
     * </p>
     *
     * @param fqn the fully qualified annotation type name to apply to the DTO, or {@code null} to indicate none
     * @throws IllegalStateException if a different marker has already been recorded for this group
     */
    public void mergeClassAnnotation(@Nullable final String fqn) {
        if (this.classAnnotationFqn == null) {
            this.classAnnotationFqn = fqn;
        } else if (!Objects.equals(this.classAnnotationFqn, fqn)) {
            throw new IllegalStateException(
                    "Conflicting classAnnotation values within group '" + this.alias + "': '"
                            + this.classAnnotationFqn + "' vs '" + fqn + "'"
            );
        }
    }

    /**
     * Sorts the group's fields deterministically by primary {@code order} (ascending),
     * then by the zero-based source declaration index.
     * <p>Invoke this before emitting the DTO to ensure stable output.</p>
     */
    public void sort() {
        this.fields.sort(Comparator
                .comparingInt(DtoField::getOrder)
                .thenComparingInt(DtoField::getDeclIndex));
    }
}
