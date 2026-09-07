package ca.bazlur.modern.concurrency.c02;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ResourcePoolTest {

  public static void main(String[] args) throws Exception {
    int maxConcurrentThreads = 5;
    int totalRequests = 50;
    var pool = new MonitoredResourcePool(maxConcurrentThreads);

    var futures = new ArrayList<Future<Optional<String>>>();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < totalRequests; i++) {
        final int taskId = i;
        futures.add(executor.submit(() -> pool.useResource("Query " + taskId)));
      }

      int successCount = 0;
      int timeoutCount = 0;

      for (Future<Optional<String>> future : futures) {
        Optional<String> result = future.get();
        if (result.isPresent()) {
          successCount++;
        } else {
          timeoutCount++; // 1. Отслеживание тайм-аутов помогает выяснить, как ведет себя система под нагрузкой.
        }
      }

      System.out.printf("requests  : %d successful: %d timed-out : %d peak usage: %d%n",
          totalRequests, successCount, timeoutCount, pool.getPeakConnections());// 2. Пиковое количество подключений
        // подтверждает корректность работы ограничения.

      assert pool.getPeakConnections() <= maxConcurrentThreads
          : "Peak connections exceeded limit!";
    }
  }
}
