package com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.adapter;
import com.swiftapp_backend.paymentservice.payment.application.port.PaymentUnitOfWork;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.function.Supplier;
@Component
public class JpaPaymentUnitOfWork implements PaymentUnitOfWork {
    private final TransactionTemplate transactions;
    private final TransactionTemplate outside;
    public JpaPaymentUnitOfWork(PlatformTransactionManager manager) {
        transactions = new TransactionTemplate(manager);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        outside = new TransactionTemplate(manager);
        outside.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    }
    @Override public <T> T execute(Supplier<T> work) { return transactions.execute(status -> work.get()); }
    @Override public <T> T outside(Supplier<T> work) { return outside.execute(status -> work.get()); }
}
