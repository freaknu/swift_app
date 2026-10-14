package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.adapter;

import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import com.swiftapp_backend.riderservice.ride.domain.model.OfferModel;
import com.swiftapp_backend.riderservice.ride.domain.repository.OfferRepository;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.mapper.RidePersistenceMapper;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository.JpaOfferRepository;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository.JpaRideRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import com.swiftapp_backend.riderservice.shared.ApiException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfferRepositoryAdapter implements OfferRepository {
    private final JpaOfferRepository repository;
    private final JpaRideRepository rides;
    private final RidePersistenceMapper mapper;
    private final EntityManager entityManager;

    @Override
    public List<OfferModel> findActive() {
        return repository.findByActiveTrue().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<OfferModel> findById(Long offerId) {
        return repository.findById(offerId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<OfferModel> findByIdForUpdate(Long offerId) {
        try {
            entityManager.unwrap(Session.class).doWork(connection -> {
                if ("PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
                    try (var statement = connection.createStatement()) {
                        // Transaction-local: never leaks into another pooled connection's request.
                        statement.execute("SET LOCAL lock_timeout = '3s'");
                    }
                }
            });
            return repository.findByIdForUpdate(offerId).map(entity -> {
                // A cancellation may already have loaded this offer through its ride. Refresh after
                // acquiring the lock so an intervening campaign edit cannot be overwritten by merge.
                entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
                return mapper.toDomain(entity);
            });
        } catch (RuntimeException ex) {
            if (isLockContention(ex)) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Offers are busy. Please retry in a moment.");
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public OfferModel save(OfferModel offer) {
        return mapper.toDomain(repository.save(mapper.toEntity(offer)));
    }

    @Override
    public long countUses(Long offerId, Long riderId) {
        return rides.countOfferUses(offerId, riderId);
    }

    @Override
    public boolean hasCompletedRide(Long riderId) {
        return rides.existsByRiderIdAndCurrentStatus(riderId, RideStatus.COMPLETED);
    }
    private boolean isLockContention(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof PessimisticLockingFailureException
                    || cause instanceof jakarta.persistence.LockTimeoutException
                    || cause instanceof jakarta.persistence.PessimisticLockException
                    || (cause instanceof SQLException sql && "55P03".equals(sql.getSQLState()))) {
                return true;
            }
        }
        return false;
    }

}
