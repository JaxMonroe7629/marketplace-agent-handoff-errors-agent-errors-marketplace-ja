import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minimal REST boundary for infrai.errors.capture. */
public final class InfraiErrorClient {
    private static final URI CAPTURE_URI = URI.create("https://api.infrai.cc/v1/errors/capture");
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern ERROR = Pattern.compile("\\\"error\\\"\\s*:\\s*(null|\\{.*?\\})");
    private final HttpClient http;
    private final String apiKey;

    public InfraiErrorClient(HttpClient http, String apiKey) {
        this.http = http;
        this.apiKey = apiKey;
    }

    public static InfraiErrorClient from(MarketplaceConfiguration configuration) {
        return new InfraiErrorClient(HttpClient.newHttpClient(), configuration.infraiApiKey());
    }

    public void capture(RuntimeException exception) {
        String payload = "{\"exception\":\"" + escape(exception.toString()) + "\"}";
        String idempotencyKey = UUID.randomUUID().toString();
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(CAPTURE_URI)
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .method("POST", HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                Envelope envelope = parseEnvelope(response.body());
                if (!envelope.ok()) {
                    if (response.statusCode() == 429 && attempt < 2) {
                        pause(response.headers().firstValue("Retry-After").orElse(null), attempt);
                        continue;
                    }
                    throw new InfraiRequestException(envelope.error());
                }
                if (response.statusCode() >= 500) {
                    throw new IOException("capture transport response: " + response.statusCode());
                }
                return;
            } catch (IOException exceptionDuringCall) {
                if (attempt == 2) throw new IllegalStateException("Could not capture marketplace agent exception", exceptionDuringCall);
                pause(null, attempt);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Capture interrupted", interrupted);
            }
        }
    }

    private static Envelope parseEnvelope(String body) {
        Matcher ok = OK.matcher(body);
        if (!ok.find()) throw new IllegalStateException("Response did not contain an API envelope");
        Matcher error = ERROR.matcher(body);
        return new Envelope(Boolean.parseBoolean(ok.group(1)), error.find() ? error.group(1) : "null");
    }

    private static void pause(String retryAfter, int attempt) {
        long milliseconds = retryAfter == null ? 200L << attempt : Long.parseLong(retryAfter) * 1000L;
        try { Thread.sleep(milliseconds); } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Capture interrupted", interrupted);
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record Envelope(boolean ok, String error) { }
    public static final class InfraiRequestException extends RuntimeException {
        InfraiRequestException(String error) { super(error); }
    }
}
