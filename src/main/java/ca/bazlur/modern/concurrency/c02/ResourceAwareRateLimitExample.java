package ca.bazlur.modern.concurrency.c02;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class ResourceAwareRateLimitExample {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10)) // конфигурация HTTP клиент
        .version(HttpClient.Version.HTTP_1_1) // <-- ДОБАВИТЬ ЭТУ СТРОКУ
        .build();

    private static final int MAX_PARALLEL = 10; // максимальное количество параллельных потоков
    private static final Semaphore gate = new Semaphore(MAX_PARALLEL); // ограничитель скорости
    private static final String API_URL =
        "https://api.chucknorris.io/jokes/random";

    public static void main(String[] args) throws Exception {
        Instant start = Instant.now();
        List<String> jokes = fetchJokes(50); // пробуем извлечь 50 лалочек, как раз посмотрим как семафор
        // будет ограничивать нас

        long ms = Duration.between(start, Instant.now()).toMillis();
        System.out.printf("Fetched %d jokes in %d ms (avg %d ms)%n",
            jokes.size(), ms, ms / jokes.size());

        jokes.stream().limit(3).forEach(j -> System.out.println("• " + j));
    }

    private static List<String> fetchJokes(int n) {
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) { // Создаем исполнитель, порождающий
            // новый виртуальный поток для каждой задачи
            List<Future<String>> futures = IntStream.range(0, n)
                .mapToObj(i -> pool.submit(ResourceAwareRateLimitExample::fetchJoke))
                .toList();

            return futures.stream()
                .map(ResourceAwareRateLimitExample::join) // Блокируем до завершения всех Future,
            // собирая результаты в порядке их выполнения.
                .toList();
        }
    }

    private static String fetchJoke() throws Exception {
        // 5 секунд достаточно, чтобы понять, что сеть "тупит", и не ждать 30
        HttpRequest req = HttpRequest.newBuilder(URI.create(API_URL))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                gate.acquire(); // Берем слот в семафоре
                try {
                    HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
                    if (res.statusCode() != 200) {
                        throw new RuntimeException("API error " + res.statusCode());
                    }
                    return parseJoke(res.body());
                } finally {
                    gate.release(); // Гарантированно освобождаем слот, даже если был таймаут
                }
            } catch (IOException | InterruptedException e) {
                // Ловим обрывы (EOF), таймауты и прерывания потоков
                if (attempt == maxRetries) {
                    // Если это последняя попытка, пробрасываем ошибку дальше
                    throw new RuntimeException("Не удалось получить шутку после " + maxRetries + " попыток", e);
                }

                // Логируем для наглядности (в реальном проекте лучше использовать logger)
                System.out.printf("⚠️ Попытка %d провалилась (%s). Повтор через 500 мс...%n",
                        attempt, e.getClass().getSimpleName());

                // Небольшая пауза, чтобы не спамить сеть и дать ей "остыть"
                Thread.sleep(500);
            }
        }
        throw new RuntimeException("Недостижимый код");
    }

    private static String parseJoke(String json) { // Выполняем упрощенный парсинг JSON для демонстрации.
        int s = json.indexOf("\"value\":\"") + 9;
        int e = json.indexOf('"', s);
        return json.substring(s, e).replace("\\\"", "\"");
    }

    private static <T> T join(Future<T> f) {
        try {
            return f.get();
        } catch (InterruptedException e) {
            /**
             *  Выполняем улучшенную обработку ошибок с сохранением статуса прерывания и разворачиваем исключение
             *  из ExecutionException, чтобы узнать его первопричину.
             */
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        }
    }
}
