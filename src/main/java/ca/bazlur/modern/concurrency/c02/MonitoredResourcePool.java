package ca.bazlur.modern.concurrency.c02;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MonitoredResourcePool {

    private final Semaphore semaphore;
    private final AtomicInteger activeConnections; // 1 Мониторинг активных подключений
    private final AtomicInteger peakConnections; // 2 Информация о периодах пиковой нагрузки критически важна для планирования ресурсов

    public MonitoredResourcePool(int resourceCount) {
        // 3 Справедливое распределение предотвращает голодание (starvation) потоков в сценариях высокой конкурентности.
        this.semaphore = new Semaphore(resourceCount, true);
        this.activeConnections = new AtomicInteger(0);
        this.peakConnections = new AtomicInteger(0);
    }

    public Optional<String> useResource(String query) {
        boolean acquired = false;
        try {
            // 4 Тайм-ауты исключают бесконечную блокировку и повышают отказоустойчивость
            acquired = semaphore.tryAcquire(5, TimeUnit.SECONDS);
            if (!acquired) {
                return Optional.empty(); // 5 если получить ресурс не удалось
            }

            int current = activeConnections.incrementAndGet();
            peakConnections.updateAndGet(peak -> Math.max(peak, current)); // обновление через атомарные операции

            return queryDatabase(query);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            if (acquired) {
                activeConnections.decrementAndGet();
                semaphore.release();
            }
        }
    }

    public int getCurrentActiveConnections() {
        return activeConnections.get();
    }

    public int getPeakConnections() {
        return peakConnections.get(); // 7 получение метрик для обеспечения наблюдательности
    }

    private Optional<String> queryDatabase(String query) {
        try {
            Thread.sleep(new Random().nextInt(500) + 500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
        return Optional.of("Result for: " + query);
    }
}
