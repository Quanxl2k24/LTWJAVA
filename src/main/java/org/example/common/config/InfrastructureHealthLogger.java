package org.example.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Slf4j
@Component
@RequiredArgsConstructor
public class InfrastructureHealthLogger {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    @EventListener(ApplicationReadyEvent.class)
    public void checkConnectionsOnStartup() {
        log.info("=================================================================");
        log.info("🔄 ĐANG KIỂM TRA TRẠNG THÁI KẾT NỐI DATABASE VÀ REDIS...");

        boolean dbSuccess = checkDatabaseConnection();
        boolean redisSuccess = checkRedisConnection();

        if (dbSuccess && redisSuccess) {
            log.info("=================================================================");
            log.info("🎉 [SUCCESS] KẾT NỐI THÀNH CÔNG ĐỒNG THỜI CẢ 2 DỊCH VỤ: MYSQL VÀ REDIS!");
            log.info("=================================================================");
        } else {
            log.warn("=================================================================");
            log.warn("⚠️ [WARNING] CHƯA THỂ KẾT NỐI ĐẦY ĐỦ CẢ 2 DỊCH VỤ (DB: {}, Redis: {}).", 
                    dbSuccess ? "OK" : "FAILED", redisSuccess ? "OK" : "FAILED");
            log.warn("   -> Vui lòng chạy lệnh: docker compose up -d để khởi động MySQL và Redis");
            log.warn("=================================================================");
        }
    }

    private boolean checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String url = metaData.getURL();
            String dbProduct = metaData.getDatabaseProductName();
            String dbVersion = metaData.getDatabaseProductVersion();
            log.info("✅ [DATABASE] Đã kết nối thành công tới Database: {} (Version: {})", dbProduct, dbVersion);
            log.info("   -> JDBC URL: {}", url);
            return true;
        } catch (Exception e) {
            log.error("❌ [DATABASE] Kết nối Database THẤT BẠI: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkRedisConnection() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pingResult = connection.ping();
            log.info("✅ [REDIS] Đã kết nối thành công tới Redis Cache Server!");
            log.info("   -> Ping response: {}", pingResult);
            return true;
        } catch (Exception e) {
            log.error("❌ [REDIS] Kết nối Redis THẤT BẠI: {}", e.getMessage());
            return false;
        }
    }
}

