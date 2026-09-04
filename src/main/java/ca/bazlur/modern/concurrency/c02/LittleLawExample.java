package ca.bazlur.modern.concurrency.c02;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class LittleLawExample {

    public static void main(String[] args) {
        int numTasks = 10000; // 1
        int avgResponseTimeMillis = 500; // Среднее время отклика // 2

        // Simulate adjustable I/O-bound work
        Runnable ioBoundTask = () -> {
            try {
                // имитирует блокирующие операции ввода-вывода
                Thread.sleep(Duration.ofMillis(avgResponseTimeMillis)); // 3
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        System.out.println("=== Little's Law Throughput Comparison ===");
        System.out.println("Testing " + numTasks + " tasks with "
            + avgResponseTimeMillis + "ms latency each\n");

        benchmark("Virtual Threads",
            Executors.newVirtualThreadPerTaskExecutor(), ioBoundTask, numTasks);
        benchmark("Fixed ThreadPool (100)",
            Executors.newFixedThreadPool(100), ioBoundTask, numTasks);
        benchmark("Fixed ThreadPool (500)",
            Executors.newFixedThreadPool(500), ioBoundTask, numTasks);
        benchmark("Fixed ThreadPool (1000)",
            Executors.newFixedThreadPool(1000), ioBoundTask, numTasks);
    }

    static void benchmark(String type, ExecutorService executor, Runnable task,
                          int numTasks) {
        Instant start = Instant.now(); // 4 запоминаем начало таймера
        AtomicLong completedTasks = new AtomicLong();

        try (executor) { // 5
            IntStream.range(0, numTasks)
                .forEach(i -> executor.submit(() -> {
                    task.run();
                    completedTasks.incrementAndGet(); // 6
                }));
        } // 7

        Instant end = Instant.now();
        long duration = Duration.between(start, end).toMillis();
        // Tasks per second
        double throughput = (double) completedTasks.get() / duration * 1000; // 8

        System.out.printf("%-25s - Time: %5dms, Throughput: %8.2f tasks/s%n", type, duration, throughput);
    }
}
