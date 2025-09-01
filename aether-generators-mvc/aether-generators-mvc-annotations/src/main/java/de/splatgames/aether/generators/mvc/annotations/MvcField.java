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

package de.splatgames.aether.generators.mvc.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Field-level customization for the generated MVC builder.
 *
 * <h2>Common uses</h2>
 * <ul>
 *   <li>Exclude a field from the builder API via {@link #ignore()}.</li>
 *   <li>Rename the fluent setter via {@link #alias()} (e.g., {@code withInvoiceTitle(...)}).</li>
 *   <li>Represent a relation field as an identifier via {@link #asIdOnly()} (generate
 *       {@code withEmployeeId(...)} instead of {@code withEmployee(...)}).</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
@Documented
@Target(FIELD)
@Retention(SOURCE)
public @interface MvcField {

    /**
     * Custom base name for the generated setter.
     * <p>Example: {@code alias = "invoiceTitle"} generates {@code withInvoiceTitle(...)}.</p>
     */
    String alias() default "";

    /**
     * Excludes this field from the builder API.
     */
    boolean ignore() default false;

    /**
     * Indicates that this relation should be exposed as a simple identifier
     * (e.g., {@code Long employeeId}) instead of the object reference
     * (e.g., {@code Employee employee}).
     */
    boolean asIdOnly() default false;
}
