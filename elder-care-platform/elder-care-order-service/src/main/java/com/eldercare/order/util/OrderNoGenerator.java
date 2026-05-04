package com.eldercare.order.util;

import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单号生成器。
 * 说明：使用简化雪花算法生成趋势递增 ID，再加 EC+日期前缀，降低多实例冲突概率。
 */
@Component
public class OrderNoGenerator {
    /** 起始时间戳，使用 2026-01-01 作为本项目纪元。 */
    private static final long EPOCH = 1767225600000L;
    /** 同一毫秒内序列号最大值。 */
    private static final long SEQUENCE_MASK = 4095L;
    /** 机器号，第一版从进程名哈希取低 10 位。 */
    private final long workerId = Math.abs(ManagementFactory.getRuntimeMXBean().getName().hashCode()) & 1023L;
    /** 上一次生成 ID 的毫秒时间戳。 */
    private long lastTimestamp = -1L;
    /** 同一毫秒内的序列号。 */
    private long sequence = 0L;
    /** 日期格式化器。 */
    private final DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE;

    /**
     * 生成订单号。
     *
     * @return 订单号，格式 ECyyyyMMdd + 雪花 ID。
     */
    public synchronized String nextOrderNo() {
        // 获取当前毫秒。
        long timestamp = System.currentTimeMillis();
        // 系统时钟回拨时使用上一毫秒，避免生成倒退 ID。
        if (timestamp < lastTimestamp) {
            // 回拨不超过短时间时等待到上一毫秒之后。
            timestamp = waitNextMillis(lastTimestamp);
        }
        // 同一毫秒内递增序列。
        if (timestamp == lastTimestamp) {
            // 序列号加一并截断到 12 位。
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 当前毫秒序列用完时等待下一毫秒。
            if (sequence == 0) {
                // 等待下一毫秒。
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 新毫秒序列号归零。
            sequence = 0L;
        }
        // 记录最近一次时间戳。
        lastTimestamp = timestamp;
        // 拼接雪花 ID。
        long snowflakeId = ((timestamp - EPOCH) << 22) | (workerId << 12) | sequence;
        // 返回业务订单号。
        return "EC" + LocalDate.now().format(formatter) + snowflakeId;
    }

    /**
     * 等待到下一毫秒。
     */
    private long waitNextMillis(long lastTimestamp) {
        // 读取当前时间。
        long timestamp = System.currentTimeMillis();
        // 循环直到时间前进。
        while (timestamp <= lastTimestamp) {
            // 继续读取当前时间。
            timestamp = System.currentTimeMillis();
        }
        // 返回新的毫秒时间。
        return timestamp;
    }
}
