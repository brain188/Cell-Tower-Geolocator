package cm.antic.cell_geolocator.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cm.antic.cell_geolocator.entity.User;
    
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
