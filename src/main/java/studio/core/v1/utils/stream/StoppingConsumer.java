package studio.core.v1.utils.stream;

import studio.core.v1.exception.StoryTellerException;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Stop parallel stream on StoryTellerException.
 *
 * @param <T> function consumer
 * @param <E> checked exception
 * @see https://dzone.com/articles/how-to-handle-checked-exception-in-lambda-expressi
 */
public interface StoppingConsumer<T> {

    void accept(T t) throws StoryTellerException;

    static <T> Consumer<T> stopped(StoppingConsumer<T> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (StoryTellerException e) {
                // Stop current parallel stream (if any)
                if (!ForkJoinPool.commonPool().isQuiescent()) {
                    ForkJoinPool.commonPool().awaitQuiescence(10, TimeUnit.SECONDS);
                }
                // re-throw exception
                throw e;
            }
        };
    }
}
