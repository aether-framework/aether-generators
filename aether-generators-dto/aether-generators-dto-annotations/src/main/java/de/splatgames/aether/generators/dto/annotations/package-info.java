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

/**
 * DTO annotation API for the Aether Generators project.
 *
 * <p>This package contains annotations used to mark fields for inclusion
 * in generated <em>Data Transfer Objects</em> (DTOs). The main entry point
 * is {@link de.splatgames.aether.generators.dto.annotations.Dto} which
 * allows developers to:</p>
 *
 * <ul>
 *   <li>Mark specific fields of an entity class for DTO generation.</li>
 *   <li>Control the generated DTO class name via {@link de.splatgames.aether.generators.dto.annotations.Dto#value()}.</li>
 *   <li>Define field ordering using {@link de.splatgames.aether.generators.dto.annotations.Dto#order()}.</li>
 *   <li>Optionally add a marker annotation to the generated DTO class
 *       using {@link de.splatgames.aether.generators.dto.annotations.Dto#classAnnotation()}.</li>
 * </ul>
 *
 * <h2>Processing</h2>
 * <p>The annotations in this package are processed at <strong>compile time</strong>
 * by the {@code aether-generators-dto-processor} module. The resulting DTO
 * classes are written into the configured <em>generated-sources</em> directory
 * (e.g., {@code target/generated-sources/apt}) so they are compiled together
 * with the project’s sources.</p>
 *
 * <h2>Integration</h2>
 * <p>To use DTO generation in a Maven project, add the following dependencies:</p>
 * <pre>{@code
 * <dependencyManagement>
 *   <dependencies>
 *     <dependency>
 *       <groupId>de.splatgames.aether</groupId>
 *       <artifactId>aether-generators-bom</artifactId>
 *       <version>1.1.2</version>
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
 * }</pre>
 *
 * @since 1.0.0
 */
package de.splatgames.aether.generators.dto.annotations;
