package de.splatgames.aether.generators.mvc.example.repository;

import de.splatgames.aether.generators.mvc.example.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
