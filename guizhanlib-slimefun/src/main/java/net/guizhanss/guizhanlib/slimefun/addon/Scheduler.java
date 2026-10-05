package net.guizhanss.guizhanlib.slimefun.addon;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.TimeUnit;

/**
 * A class for scheduling tasks.
 * Modified from InfinityLib
 * <p>
 * On Folia, "global" tasks are scheduled on the global region scheduler,
 * entity/location tasks are scheduled on the owning entity/region scheduler.
 * All delay/interval parameters are in ticks, converted internally where needed.
 * Region/entity tasks always incur at least 1 tick of delay on Folia
 * (inherent to the scheduling model), so delays are clamped to a minimum of 1 tick.
 *
 * @author Mooy1
 * @author ybw0014
 */
@ParametersAreNonnullByDefault
@SuppressWarnings("ConstantConditions")
public final class Scheduler {

    // Folia detection, checked once and cached
    private static volatile Boolean isFolia;

    private final Plugin plugin;

    public Scheduler(@Nonnull Plugin plugin) {
        Preconditions.checkArgument(plugin != null, "Plugin instance cannot be null");
        this.plugin = plugin;
    }

    /**
     * Whether the server is a Folia-based server.
     * The detection is performed once and cached.
     *
     * @return Whether the server is Folia-based.
     */
    public static boolean isFolia() {
        Boolean folia = isFolia;
        if (folia != null) {
            return folia;
        }
        synchronized (Scheduler.class) {
            if (isFolia == null) {
                try {
                    Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                    isFolia = true;
                } catch (ClassNotFoundException e) {
                    isFolia = false;
                }
            }
            return isFolia;
        }
    }

    public void run(@Nonnull Runnable runnable) {
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> runnable.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public void runAsync(@Nonnull Runnable runnable) {
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runNow(plugin, t -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public void run(int delayTicks, @Nonnull Runnable runnable) {
        int delay = Math.max(1, delayTicks);
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> runnable.run(), delay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        }
    }

    public void runAsync(int delayTicks, @Nonnull Runnable runnable) {
        long delayInMillis = Math.max(1, delayTicks) * 50L;
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runDelayed(plugin, t -> runnable.run(), delayInMillis, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayTicks);
        }
    }

    public void repeat(int intervalTicks, @Nonnull Runnable runnable) {
        repeat(intervalTicks, 1, runnable);
    }

    public void repeatAsync(int intervalTicks, @Nonnull Runnable runnable) {
        repeatAsync(intervalTicks, 1, runnable);
    }

    public void repeat(int intervalTicks, int delayTicks, @Nonnull Runnable runnable) {
        long interval = Math.max(1, intervalTicks);
        long delay = Math.max(1, delayTicks);
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> runnable.run(), delay, interval);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, interval);
        }
    }

    public void repeatAsync(int intervalTicks, int delayTicks, @Nonnull Runnable runnable) {
        long delayInMillis = Math.max(1, delayTicks) * 50L;
        long intervalInMillis = Math.max(1, intervalTicks) * 50L;
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> runnable.run(), delayInMillis, intervalInMillis, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, intervalTicks);
        }
    }

    /**
     * Runs a task on the scheduler of the given entity (Folia) or the main thread (Paper).
     * The task runs immediately on the entity's thread.
     *
     * @param entity    the {@link Entity} which owns the task
     * @param runnable  the task to run
     */
    public void runAtEntity(@Nonnull Entity entity, @Nonnull Runnable runnable) {
        if (isFolia()) {
            try {
                entity.getScheduler().run(plugin, t -> runnable.run(), null);
            } catch (IllegalStateException e) {
                // the entity was retired before the task could be scheduled
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Runs a task on the scheduler of the given entity after at least 1 tick.
     *
     * @param entity    the {@link Entity} which owns the task
     * @param delayTicks the delay in ticks
     * @param runnable  the task to run
     */
    public void runAtEntity(@Nonnull Entity entity, int delayTicks, @Nonnull Runnable runnable) {
        int delay = Math.max(1, delayTicks);
        if (isFolia()) {
            try {
                entity.getScheduler().runDelayed(plugin, t -> runnable.run(), null, delay);
            } catch (IllegalStateException e) {
                // the entity was retired before the task could be scheduled
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        }
    }

    /**
     * Runs a task on the region owning the given location (Folia) or the main thread (Paper).
     * The task runs on the next tick of the region.
     *
     * @param location the {@link Location} which determines the owning region
     * @param runnable the task to run
     */
    public void runAtRegion(@Nonnull Location location, @Nonnull Runnable runnable) {
        if (isFolia()) {
            Bukkit.getRegionScheduler().run(plugin, location, t -> runnable.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Runs a task on the region owning the given location after at least 1 tick.
     *
     * @param location   the {@link Location} which determines the owning region
     * @param delayTicks the delay in ticks
     * @param runnable   the task to run
     */
    public void runAtRegion(@Nonnull Location location, int delayTicks, @Nonnull Runnable runnable) {
        int delay = Math.max(1, delayTicks);
        if (isFolia()) {
            Bukkit.getRegionScheduler().runDelayed(plugin, location, t -> runnable.run(), delay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        }
    }
}
