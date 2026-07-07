package com.example.resort.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    // Mỗi IP có 1 bucket riêng — lưu trong memory
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    // Lấy hoặc tạo bucket cho IP
    private Bucket getBucket(String ip, String path)
    {
        String key = ip + path;
        return bucketCache.computeIfAbsent(key, k -> createBucket(path));
    }

    // Tạo bucket với giới hạn tùy theo endpoint
    private Bucket createBucket(String path)
    {
        Bandwidth limit;

        if (path.contains("/auth/token"))
        {
            // /auth/token: chỉ 5 request/phút (chống brute force)
            limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)));
        }
        else if (path.contains("/bookings") || path.contains("/Booking"))
        {
            // /Booking: 20 request/phút (chống spam đặt phòng)
            limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
        }

        else
        {
            limit = Bandwidth.classic(60, Refill.greedy(60, Duration.ofMinutes(1)));
        }
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }





    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String ip  = getClientIp(request);
        String path = request.getRequestURI();
        Bucket bucket = getBucket(ip, path);
        if (bucket.tryConsume(1))
        {
            // Còn token → cho phép request đi tiếp
            filterChain.doFilter(request, response);
        }
        else
        {
            // Hết token → chặn lại
            log.warn("RateLimit EXCEEDED - IP: {}, PATH: :{}", ip, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {
                    "code":429, 
                    "message":"Too Many Requests. Please try again"
                    }
                    """);
        }

    }

    // Lấy IP thật của client (kể cả qua proxy)
    private String getClientIp(HttpServletRequest  request)
    {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty())
            {
                ip = request.getRemoteAddr();
            }
        // X-Forwarded-For có thể chứa nhiều IP, lấy IP đầu tiên
        return ip.split(",")[0].trim();
    }
}
