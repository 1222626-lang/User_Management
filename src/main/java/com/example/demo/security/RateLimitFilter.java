
package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_SIZE =
            Duration.ofMinutes(1).toMillis();

    private static final int REQUEST_LIMIT = 5;

    private final ConcurrentHashMap<String, Window> requests =
            new ConcurrentHashMap<>();

    private static class Window {

        private final long startTime;
        private final int count;

        public Window(long startTime, int count) {
            this.startTime = startTime;
            this.count = count;
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();
        String method = request.getMethod();

        // Apply rate limiting only to POST register/login.
        if (!"POST".equalsIgnoreCase(method)
                || !isLimitedEndpoint(path)) {

            filterChain.doFilter(request, response);
            return;
        }

        String ipAddress = request.getRemoteAddr();

        String key = ipAddress + ":" + path;

        long currentTime = System.currentTimeMillis();

        AtomicBoolean exceeded = new AtomicBoolean(false);

        AtomicBoolean reset = new AtomicBoolean(false);

        requests.compute(key, (k, oldWindow) -> {

            // First request or expired window.
            if (oldWindow == null
                    || currentTime - oldWindow.startTime >= WINDOW_SIZE) {

                reset.set(true);

                return new Window(currentTime, 1);
            }

            // Limit exceeded.
            if (oldWindow.count >= REQUEST_LIMIT) {

                exceeded.set(true);

                return oldWindow;
            }

            // Increment request count.
            return new Window(
                    oldWindow.startTime,
                    oldWindow.count + 1
            );
        });

        if (exceeded.get()) {

            Window window = requests.get(key);

            long retryAfter = Math.max(
                    1,
                    (WINDOW_SIZE -
                            (currentTime - window.startTime) + 999) / 1000
            );

            response.setStatus(429);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            response.setHeader(
                    "X-RateLimit-Limit",
                    String.valueOf(REQUEST_LIMIT)
            );

            response.setHeader(
                    "X-RateLimit-Remaining",
                    "0"
            );

            response.setHeader(
                    "Retry-After",
                    String.valueOf(retryAfter)
            );

            response.getWriter().write(
                    """
                    {
                        "status": 429,
                        "error": "Too Many Requests",
                        "message": "Maximum 5 requests per minute. Try again later.",
                        "retryAfterSeconds": %d
                    }
                    """.formatted(retryAfter)
            );

            return;
        }

        Window currentWindow = requests.get(key);

        int remaining = Math.max(
                0,
                REQUEST_LIMIT - currentWindow.count
        );

        response.setHeader(
                "X-RateLimit-Limit",
                String.valueOf(REQUEST_LIMIT)
        );

        response.setHeader(
                "X-RateLimit-Remaining",
                String.valueOf(remaining)
        );

        filterChain.doFilter(request, response);
    }

    private boolean isLimitedEndpoint(String path) {

        return "/api/auth/register".equals(path)
                || "/api/auth/login".equals(path);
    }
}
