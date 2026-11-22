package net.guizhanss.guizhanlib.minecraft.plugin;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.TimeUnit;

/**
 * A Folia-aware scheduler wrapper.
 * <p>
 * On Folia, global tasks use the global region scheduler, async tasks use the async
 * scheduler, and entity/location tasks use the owning entity/region scheduler. On
 * Paper, the legacy Bukkit scheduler is used. Region and entity tasks always incur at
 * least one tick of delay on Folia, so delays are clamped to a minimum of one tick.
 *
 * @author Mooy1
 * @author ybw0014
 */
@ParametersAreNonnullByDefault
@SuppressWarnings({"ConstantConditions", "unused"})
public final class Scheduler {

    // Folia detection, performed once and cached
    private static volatile Boolean folia;

    private final Plugin plugin;

    /**
     * Create a new scheduler wrapper.
     *
     * @param plugin the owning plugin
     */
    public Scheduler(Plugin plugin) {
        Preconditions.checkArgument(plugin != null, "Plugin instance cannot be null");
        this.plugin = plugin;
    }

    /**
     * Check whether the server is a Folia-based server.
     *
     * @return whether the server is Folia-based
     */
    public static boolean isFolia() {
        Boolean cached = folia;
        if (cached != null) {
            return cached;
        }
        synchronized (Scheduler.class) {
            if (folia == null) {
                try {
                    Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                    folia = true;
                } catch (ClassNotFoundException ex) {
                    folia = false;
                }
            }
            return folia;
        }
    }

    /**
     * Run a task on the global region (Folia) or the main thread (Paper).
     *
     * @param runnable the task to run
     */
    public void run(Runnable runnable) {
        Preconditions.checkArgument(runnable != null, "Runnable cannot be null");
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().run(plugin, task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Run a task on the async scheduler.
     *
     * @param runnable the task to run
     */
    public void runAsync(Runnable runnable) {
        Preconditions.checkArgument(runnable != null, "Runnable cannot be null");
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    /**
     * Run a task on the global region (Folia) or the main thread (Paper) after a delay.
     *
     * @param delayTicks the delay in ticks, clamped to at least one tick
     * @param runnable   the task to run
     */
    public void run(int delayTicks, Runnable runnable) {
        Preconditions.checkArgument(runnable != null, "Runnable cannot be null");
        long delay = Math.max(1, delayTicks);
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> runnable.run(), delay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        }
    }

    /**
     * Run a task on the async scheduler after a delay.
     *
     * @param delayTicks the delay in ticks, clamped to at least one tick
     * @param runnable   the task to run
     */
    public void runAsync(int delayTicks, Runnable runnable) {
        Preconditions.checkArgument(runnable != null, "Runnable cannot be null");
        long delay = Math.max(1, delayTicks);
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runDelayed(plugin, task -> runnable.run(), delay * 50L, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
        }
    }

    /**
     * Run a repeating task on the global region (Folia) or the main thread (Paper).
     *
     * @param intervalTicks the interval in ticks, clamped to at least one tick
     * @param runnable      the task to run
     */
    public void repeat(int intervalTicks, Runnable runnable) {
        repeat(intervalTicks, 1, runnable);
    }

    /**
     * Run a repeating task on the async scheduler.
     *
     * @param intervalTicks the interval in ticks, clamped to at least one tick
     * @param runnable      the task to run
     */
    public void repeatAsync(int intervalTicks, Runnable runnable) {
        repeatAsync(intervalTicks, 1, runnable);
    }

    /**
     * Run a repeating task on the global region (Folia) or the main thread (Paper).
     *
     * @param intervalTicks the interval in ticks, clamped to at least one tick
     * @param delayTicks    the delay in ticks, clamped to at least one tick
     * @param runnable      the task to run
     */
    public void repeat(int intervalTicks, int delayTicks, Runnable runnable) {
        Preconditions.checkArgument(runnable != null, "Runnable cannot be null");
        long interval = Math.max(1, intervalTicks);
        long delay = Math.max(1, delayTicks);
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> runnable.run(), delay, interval);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, interval);
        }
    }

    /**
     * Run a repeating task on the async scheduler.
     *
     * @param intervalTicks the interval in ticks, clamped to at least one tick
     * @param delayTicks    the delay in ticks, clamped to at least one tick
     * @param runnable      the task to run
     */
    public void repeatAsync(int intervalTicks, int delayTicks, Runnable runnable) {
        Preconditions.checkArgument(runnable != null, "Runnable cannot be null");
        long interval = Math.max(1, intervalTicks);
        long delay = Math.max(1, delayTicks);
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> runnable.run(), delay * 50L, interval * 50L, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, interval);
        }
    }

    /**
     * Run a task on the scheduler owning the given entity.
     *
     * @param entity   the entity which owns the task
     * @param runnable the task to run
     */
    public void runAtEntity(Entity entity, Runnable runnable) {
        Preconditions.checkArgument(entity != null, "Entity cannot be null");
        Preconditions.checkArgument(runnable != null, "Runnable cannot be null");
        if (isFolia()) {
            try {
                entity.getScheduler().run(plugin, task -> runnable.run(), null);
            } catch (IllegalStateException ex) {
                // the entity was retired before the task could be scheduled
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Run a task on the scheduler owning the given entity after a delay.
     *
     * @param entity     the entity which owns the task
     * @param delayTicks the delay in ticks, clamped to at least one tick
     * @param runnable   the task to run
     */
    public void runAtEntity(Entity entity, int delayTicks, Runnable runnable) {
        Preconditions.checkArgument(entity != null, "Entity cannot be null");
        Preconditions.checkArgument(runnable != null, "Runnable cannot be null");
        long delay = Math.max(1, delayTicks);
        if (isFolia()) {
            try {
                entity.getScheduler().runDelayed(plugin, task -> runnable.run(), null, delay);
            } catch (IllegalStateException ex) {
                // the entity was retired before the task could be scheduled
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        }
    }

    /**
     * Run a task on the region owning the given location.
     *
     * @param location the location which determines the owning region
     * @param runnable the task to run
     */
    public void runAtRegion(Location location, Runnable runnable) {
        Preconditions.checkArgument(location != null, "Location cannot be null");
        Preconditions.checkArgument(runnable != null, "Runnable cannot be null");
        if (isFolia()) {
            Bukkit.getRegionScheduler().run(plugin, location, task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Run a task on the region owning the given location after a delay.
     *
     * @param location   the location which determines the owning region
     * @param delayTicks the delay in ticks, clamped to at least one tick
     * @param runnable   the task to run
     */
    public void runAtRegion(Location location, int delayTicks, Runnable runnable) {
        Preconditions.checkArgument(location != null, "Location cannot be null");
        Preconditions.checkArgument(runnable != null, "Runnable cannot be null");
        long delay = Math.max(1, delayTicks);
        if (isFolia()) {
            Bukkit.getRegionScheduler().runDelayed(plugin, location, task -> runnable.run(), delay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        }
    }
}
