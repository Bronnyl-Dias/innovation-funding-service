package org.innovateuk.ifs.async;

import org.innovateuk.ifs.util.ExceptionThrowingRunnable;
import org.innovateuk.ifs.util.ExceptionThrowingSupplier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Test helper to allow execution of @Async code blocks
 */
@Component
public class AsyncExecutionTestHelper {

    @Async
    public <T> CompletableFuture<T> executeAsync(ExceptionThrowingSupplier<T> supplier) {
        try {
            return CompletableFuture.completedFuture(supplier.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Async
    public CompletableFuture<Void> executeAsync(ExceptionThrowingRunnable supplier) {
        try {
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
