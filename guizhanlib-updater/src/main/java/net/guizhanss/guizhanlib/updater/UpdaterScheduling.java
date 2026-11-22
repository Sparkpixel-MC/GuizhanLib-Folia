package net.guizhanss.guizhanlib.updater;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.TimeUnit;

/**
 * Scheduling helpers shared by the updaters.
 * <p>
 * Update checks perform blocking network IO and must never run on a server tick
 * thread. On Folia they are handed to the async scheduler directly; on Paper they
 * are delayed by one tick and then moved to an async task.
 *
 * @author ybw0014
 */
@ParametersAreNonnullByDefault
final class UpdaterScheduling {

    // Folia detection, performed once and cached
    private static volatile Boolean folia;

    private UpdaterScheduling() {
    }

    /**
     * Run an updater task asynchronously after one tick.
     *
     * @param plugin the owning plugin
     * @param task   the update task
     */
    static void runUpdater(Plugin plugin, Runnable task) {
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runDelayed(plugin, scheduled -> task.run(), 50L, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.getScheduler().runTaskAsynchronously(plugin, task), 1L);
        }
    }

    private static boolean isFolia() {
        Boolean cached = folia;
        if (cached != null) {
            return cached;
        }
        synchronized (UpdaterScheduling.class) {
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
}
