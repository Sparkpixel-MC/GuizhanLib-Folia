package net.guizhanss.guizhanlib.common;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This package provides rate limit feature.
 *
 * @param <K> The type of key.
 * @author ybw0014
 */
public final class RateLimit<K> {

    private final long limitTime;
    private final int limitVisits;
    private final Map<K, Visit> visits = new ConcurrentHashMap<>(); // record of visits per key

    private record Visit(long startTime, int visits) {
    }

    /**
     * Constructor.
     *
     * @param time   Time period in milliseconds.
     * @param visits The limit times of visits within the period.
     */
    public RateLimit(long time, int visits) {
        this.limitTime = time;
        this.limitVisits = visits;
    }

    /**
     * Get the overall limit of visits.
     *
     * @return The overall limit of visits.
     */
    public int getLimit() {
        return this.limitVisits;
    }

    /**
     * Get the used visits.
     *
     * @param key The query key.
     * @return Used times of visits.
     */
    public int getUsed(@Nonnull K key) {
        Preconditions.checkArgument(key != null, "key should not be null");

        Visit visit = visits.get(key);

        // Lack of record, this should not happen
        if (visit == null) {
            return 0;
        }

        // Time out
        if (System.nanoTime() - visit.startTime() >= limitTime) {
            return 0;
        }

        return visit.visits();
    }

    /**
     * Get remaining times of visits
     *
     * @param key The query key.
     * @return Remaining times of visits.
     */
    public int getRemaining(@Nonnull K key) {
        Preconditions.checkArgument(key != null, "key should not be null");

        return getLimit() - getUsed(key);
    }

    /**
     * Increases the times of visits.
     *
     * @param key    The query key.
     * @param visits The times of visits
     * @return Whether the action succeeds.
     */
    public boolean add(@Nonnull K key, int visits) {
        Preconditions.checkArgument(key != null, "key should not be null");

        long now = System.nanoTime();
        boolean[] success = new boolean[1];

        // compute is atomic per key, so the limit check and update cannot interleave across threads
        this.visits.compute(key, (k, visit) -> {
            // Lack of record or time out, start a new period
            if (visit == null || now - visit.startTime() >= limitTime) {
                success[0] = true;
                return new Visit(now, visits);
            }

            if (limitVisits - visit.visits() < visits) {
                success[0] = false;
                return visit;
            }

            success[0] = true;
            return new Visit(visit.startTime(), visit.visits() + visits);
        });

        return success[0];
    }

    /**
     * Increases the times of visits by 1.
     *
     * @param key The query key.
     * @return Whether the action succeeds.
     */
    public boolean add(@Nonnull K key) {
        Preconditions.checkArgument(key != null, "key should not be null");

        return add(key, 1);
    }

    /**
     * Reset rate limit.
     *
     * @param key The query key.
     */
    public void reset(@Nonnull K key) {
        Preconditions.checkArgument(key != null, "key should not be null");

        visits.remove(key);
    }
}
