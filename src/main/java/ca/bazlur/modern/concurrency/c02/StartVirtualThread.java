package ca.bazlur.modern.concurrency.c02;

/**
 * Самплы создания потоков
 *
 *
 */
public class StartVirtualThread {
    static void main() throws InterruptedException  {
        Thread virtualThread = Thread.startVirtualThread(() -> {
            System.out.println("Unleash massive parallelism with virtual threads! Here's a taste.");
        });
        virtualThread.join();

        var startedThread = Thread.ofVirtual()
                .start(() -> System.out.println("Hello world!"));
        startedThread.join();

        //Чтобы создать поток без немедленного запуска, используйте такой код:
        var unstartedThread = Thread.ofVirtual()
                .unstarted(() -> System.out.println("Hello world! from unstartedThread"));
        // Запустить поток позже, когда потребуется
        unstartedThread.start();
    }
}
