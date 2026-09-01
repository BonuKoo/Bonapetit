package com.eatmate.loadtest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 부하 측정 결과 집계.
 *
 * 평균만 보면 안 된다. 채팅처럼 사람이 기다리는 경로는 <b>꼬리 지연</b>이 체감을 만든다.
 * 평균이 20ms여도 p99가 2초면 100명 중 1명은 2초를 기다린다.
 */
final class LoadStats {

    private final List<Long> latenciesMicros = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger sent = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();

    void recordSent() {
        sent.incrementAndGet();
    }

    void recordLatency(long nanos) {
        latenciesMicros.add(nanos / 1_000);
    }

    void recordFailure() {
        failed.incrementAndGet();
    }

    int sentCount() {
        return sent.get();
    }

    int receivedCount() {
        return latenciesMicros.size();
    }

    int failedCount() {
        return failed.get();
    }

    /** 응답을 받지 못한 건. 타임아웃이나 유실이다. */
    int missingCount() {
        return sentCount() - receivedCount() - failedCount();
    }

    private double percentileMillis(double p) {
        List<Long> sorted;
        synchronized (latenciesMicros) {
            sorted = new ArrayList<>(latenciesMicros);
        }
        if (sorted.isEmpty()) {
            return Double.NaN;
        }
        Collections.sort(sorted);
        int index = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1))) / 1000.0;
    }

    String report(String label, long elapsedMillis) {
        double seconds = elapsedMillis / 1000.0;
        return """
                ================ %s ================
                보낸 메시지      %d
                응답 받음        %d
                실패            %d
                응답 없음        %d
                소요            %.2f s
                처리량           %.1f msg/s
                지연 p50        %.1f ms
                지연 p95        %.1f ms
                지연 p99        %.1f ms
                지연 최대        %.1f ms
                ==========================================
                """.formatted(label, sentCount(), receivedCount(), failedCount(), missingCount(),
                seconds, seconds > 0 ? receivedCount() / seconds : 0,
                percentileMillis(50), percentileMillis(95), percentileMillis(99), percentileMillis(100));
    }
}
