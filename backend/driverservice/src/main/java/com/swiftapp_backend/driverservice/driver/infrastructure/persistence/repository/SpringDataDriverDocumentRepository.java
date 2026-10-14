package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository;

import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataDriverDocumentRepository extends JpaRepository<DriverDocumentEntity,Long> {
    List<DriverDocumentEntity> findAllByDriver_Id(Long driverId);
    // Older registrations stored the driver relation without copying authUserId.
    // Recover those uploads through their owner while keeping account isolation.
    @Query("select document from DriverDocumentModel document left join document.driver owner "
            + "where document.authUserId = :authUserId "
            + "or (document.authUserId is null and owner.authUserId = :authUserId)")
    List<DriverDocumentEntity> findAllByAuthUserId(@Param("authUserId") Long authUserId);

    @Query("select document from DriverDocumentModel document left join document.driver owner "
            + "where document.id = :id and "
            + "(document.authUserId = :authUserId or "
            + "(document.authUserId is null and owner.authUserId = :authUserId))")
    Optional<DriverDocumentEntity> findByAuthUserIdAndId(@Param("authUserId") Long authUserId,
                                                       @Param("id") Long id);
}
