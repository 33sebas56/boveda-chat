package com.seguridad.bovedachat.repository;
import com.seguridad.bovedachat.domain.StoredFile;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface FileRepository extends JpaRepository<StoredFile, UUID> {
    List<StoredFile> findByOwner_IdOrderByCreatedAtDesc(UUID ownerId);
    long countByOwner_Id(UUID ownerId);
    @Query("select coalesce(sum(f.sizeBytes),0) from StoredFile f where f.owner.id=:owner")
    long usedBytes(UUID owner);
}
