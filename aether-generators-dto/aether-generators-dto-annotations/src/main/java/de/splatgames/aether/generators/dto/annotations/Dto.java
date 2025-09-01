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

package de.splatgames.aether.generators.dto.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Indicates that the annotated field is part of a generated
 * <em>Data Transfer Object</em> (DTO) for its declaring class.
 *
 * <p>The DTO class is generated at <em>compile time</em> by an annotation
 * processor. By default the DTO is placed in the <strong>same package</strong>
 * as the declaring class and written to
 * {@code target/generated-sources/apt} so that it is compiled together with
 * the project sources.</p>
 *
 * <h2>DTO class name</h2>
 * <ul>
 *   <li>If {@link #value()} is empty, the DTO class name is
 *       {@code <DeclaringSimpleName>Dto} (e.g. {@code RoleDto}).</li>
 *   <li>If {@link #value()} is set to {@code "MyObject"}, the DTO class name
 *       is {@code MyObjectDto}.</li>
 * </ul>
 *
 * <h2>Field ordering</h2>
 * <p>Generated DTO accessors and constructor parameters follow ascending
 * {@link #order()} values. For equal order values, the processor may fall
 * back to the source declaration order.</p>
 *
 * <h2>Generated DTO characteristics</h2>
 * <ul>
 *   <li>Implements {@link java.io.Serializable}.</li>
 *   <li>Provides getters and a canonical constructor covering all included fields.</li>
 *   <li>Provides {@code equals}, {@code hashCode}, and {@code toString} based on included fields.</li>
 *   <li>Optionally adds a marker annotation to the DTO type when {@link #classAnnotation()} is set.</li>
 * </ul>
 *
 * <h2>Type-level marker annotation on the DTO</h2>
 * <p>If {@link #classAnnotation()} is a non-empty fully qualified annotation
 * type name (e.g. {@code lombok.Data}), the generator emits
 * {@code @lombok.Data} on the generated DTO class. The processor treats the
 * value as a <em>marker</em> annotation without parameters.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Source class:
 * package com.example.roles;
 *
 * public class Role {
 *     @Dto(order = 1) private String name;
 *     @Dto(order = 2) private boolean isDefault;
 * }
 *
 * // Generated into: target/generated-sources/apt/com/example/roles/RoleDto.java
 * // package com.example.roles;
 * // public final class RoleDto implements Serializable { ... }
 * }</pre>
 *
 * <h2>Build integration (Maven)</h2>
 * <p>Add the annotations and the processor to your project and direct generated sources to {@code apt}:</p>
 * <pre>{@code
 * <dependencyManagement>
 *   <dependencies>
 *     <dependency>
 *       <groupId>de.splatgames.aether</groupId>
 *       <artifactId>aether-generators-bom</artifactId>
 *       <version>1.1.1</version>
 *       <type>pom</type>
 *       <scope>import</scope>
 *     </dependency>
 *   </dependencies>
 * </dependencyManagement>
 *
 * <dependencies>
 *   <dependency>
 *     <groupId>de.splatgames.aether</groupId>
 *     <artifactId>aether-generators-dto-annotations</artifactId>
 *     <scope>provided</scope>
 *   </dependency>
 * </dependencies>
 *
 * <build>
 *   <plugins>
 *     <plugin>
 *       <groupId>org.apache.maven.plugins</groupId>
 *       <artifactId>maven-compiler-plugin</artifactId>
 *       <version>3.11.0</version>
 *       <configuration>
 *         <release>17</release>
 *         <annotationProcessorPaths>
 *           <path>
 *             <groupId>de.splatgames.aether</groupId>
 *             <artifactId>aether-generators-dto-processor</artifactId>
 *             <version>1.1.1</version>
 *           </path>
 *         </annotationProcessorPaths>
 *         <generatedSourcesDirectory>
 *           ${project.build.directory}/generated-sources/apt
 *         </generatedSourcesDirectory>
 *       </configuration>
 *     </plugin>
 *   </plugins>
 * </build>
 * }</pre>
 *
 * @apiNote
 * This annotation is processed at compile time only and is not retained at runtime.
 * The baseline is Java 17; artifacts compiled with {@code --release 17} run on newer JREs (e.g., 21, 25).
 *
 * @implNote
 * Only fields annotated with {@code @Dto} are included in the DTO. Non-annotated fields are ignored.
 * The processor may emit simple Javadoc that references the original declaring class and fields.
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
@Documented
@Target(FIELD)
@Retention(SOURCE)
public @interface Dto {

    /**
     * Optional base name for the generated DTO class.
     * <p>If empty, the DTO class name defaults to
     * {@code <DeclaringSimpleName>Dto}.</p>
     *
     * @return the custom base name for the DTO class (without the {@code Dto} suffix)
     */
    String value() default "";

    /**
     * Order key used to sort fields in the generated DTO constructor and accessors.
     * <p>Lower values come first. Ties may fall back to the source declaration order.</p>
     *
     * @return the ordering key (ascending)
     */
    int order();

    /**
     * Fully qualified name of a <em>marker</em> annotation to be applied to the generated DTO class.
     * <p>Example: {@code "lombok.Data"} results in {@code @lombok.Data} on the DTO type.
     * Parameters are not supported.</p>
     *
     * @return the fully qualified annotation type name, or an empty string to disable
     */
    String classAnnotation() default "";
}
