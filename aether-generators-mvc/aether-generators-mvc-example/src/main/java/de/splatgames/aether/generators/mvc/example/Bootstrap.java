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

package de.splatgames.aether.generators.mvc.example;

import de.splatgames.aether.generators.mvc.adapter.spring.SpringPersistAdapter;
import de.splatgames.aether.generators.mvc.example.domain.Department;
import de.splatgames.aether.generators.mvc.example.domain.Employee;
import de.splatgames.aether.generators.mvc.example.domain.EmployeeBuilder;
import de.splatgames.aether.generators.mvc.example.repository.DepartmentRepository;
import de.splatgames.aether.generators.mvc.example.repository.EmployeeRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class Bootstrap {

    @Bean
    CommandLineRunner demo(@NotNull final ApplicationContext ctx,
                           @NotNull final DepartmentRepository departments,
                           @NotNull final EmployeeRepository employees) {
        return args -> {
            // 1) Seed a Department
            Department dept = new Department("Engineering");
            dept = departments.save(dept);

            // 2) Use the generated EmployeeBuilder
            //    Note: class appears after compilation, package = same as Employee
            var adapter = new SpringPersistAdapter(ctx);
            var builder = new EmployeeBuilder(adapter)
                    .persistent()
                    .withName("Alice")
                    .withActive(true)
                    .withDepartmentId(dept.getId()) // asIdOnly usage
                    .addTag("java")
                    .addTag("spring");

            Employee e = builder.create();
            System.out.println("Saved employee id=" + e.getId());

            // 3) And build transient instances as well
            var transientEmp = new EmployeeBuilder()
                    .transientMode()
                    .withName("Bob")
                    .withActive(false)
                    .addTag("no-persist")
                    .create();

            System.out.println("Transient employee has id=" + transientEmp.getId()); // null
        };
    }
}