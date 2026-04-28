package com.vitorcamprubi.sgtc.repo;

import com.vitorcamprubi.sgtc.domain.Role;
import com.vitorcamprubi.sgtc.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
<<<<<<< HEAD
    Optional<User> findByTokenConfirmacao(String tokenConfirmacao);
=======
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
}
