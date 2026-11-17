package com.seguridad.bovedachat.repository;
import com.seguridad.bovedachat.domain.Account;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByUsername(String username);
    Optional<Account> findByPhone(String phone);
    boolean existsByUsername(String username);
    boolean existsByPhone(String phone);
    @Query("select a from Account a where a.enabled=true and a.id<>:exclude and locate(:prefix,a.username)=1 order by a.username")
    List<Account> search(String prefix, UUID exclude, Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id=:id")
    Optional<Account> lockById(UUID id);
}
