package de.splatgames.aether.generators.mvc.example.repository;

import de.splatgames.aether.generators.mvc.example.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
