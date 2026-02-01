package com.serverstats.paper.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import java.util.concurrent.TimeUnit;

/**
 * Utility class that abstracts scheduling to support both Paper/Spigot and Folia.
 * Automatically detects the server type and uses the appropriate scheduler.
 */
public final class SchedulerUtil {

  private static final boolean IS_FOLIA;

  static {
    boolean folia;
    try {
      Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
      folia = true;
    } catch (ClassNotFoundException e) {
      folia = false;
    }
    IS_FOLIA = folia;
  }

  private SchedulerUtil() {
    // Utility class
  }

  /**
   * Check if the server is running Folia
   *
   * @return true if running Folia
   */
  public static boolean isFolia() {
    return IS_FOLIA;
  }

  /**
   * Run a task on the main/global thread.
   * On Paper/Spigot: runs on the main thread.
   * On Folia: runs on the global region scheduler.
   *
   * @param plugin The plugin
   * @param task The task to run
   */
  public static void runSync(Plugin plugin, Runnable task) {
    if (IS_FOLIA) {
      Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
    } else {
      Bukkit.getScheduler().runTask(plugin, task);
    }
  }

  /**
   * Run a task on the main/global thread after a delay.
   * On Paper/Spigot: runs on the main thread.
   * On Folia: runs on the global region scheduler.
   *
   * @param plugin The plugin
   * @param task The task to run
   * @param delayTicks The delay in ticks
   */
  public static void runSyncDelayed(Plugin plugin, Runnable task, long delayTicks) {
    if (IS_FOLIA) {
      Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayTicks);
    } else {
      Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }
  }

  /**
   * Run a task tied to an entity's region thread.
   * On Paper/Spigot: runs on the main thread.
   * On Folia: runs on the entity's region scheduler.
   *
   * @param plugin The plugin
   * @param entity The entity to run the task for
   * @param task The task to run
   * @param retired Runnable to execute if the entity is retired (removed) before the task runs
   */
  public static void runForEntity(Plugin plugin, Entity entity, Runnable task, Runnable retired) {
    if (IS_FOLIA) {
      entity.getScheduler().run(plugin, scheduledTask -> task.run(), retired);
    } else {
      Bukkit.getScheduler().runTask(plugin, task);
    }
  }

  /**
   * Run a task tied to an entity's region thread after a delay.
   * On Paper/Spigot: runs on the main thread.
   * On Folia: runs on the entity's region scheduler.
   *
   * @param plugin The plugin
   * @param entity The entity to run the task for
   * @param task The task to run
   * @param retired Runnable to execute if the entity is retired (removed) before the task runs
   * @param delayTicks The delay in ticks
   */
  public static void runForEntityDelayed(Plugin plugin, Entity entity, Runnable task, Runnable retired, long delayTicks) {
    if (IS_FOLIA) {
      entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), retired, delayTicks);
    } else {
      Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }
  }

  /**
   * Run a task asynchronously.
   *
   * @param plugin The plugin
   * @param task The task to run
   */
  public static void runAsync(Plugin plugin, Runnable task) {
    if (IS_FOLIA) {
      Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
    } else {
      Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }
  }

  /**
   * Run a task asynchronously after a delay.
   *
   * @param plugin The plugin
   * @param task The task to run
   * @param delayTicks The delay in ticks (converted to milliseconds for Folia)
   */
  public static void runAsyncDelayed(Plugin plugin, Runnable task, long delayTicks) {
    if (IS_FOLIA) {
      // Convert ticks to milliseconds (1 tick = 50ms)
      long delayMs = delayTicks * 50;
      Bukkit.getAsyncScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayMs, TimeUnit.MILLISECONDS);
    } else {
      Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
    }
  }

  /**
   * Schedule a repeating async task.
   *
   * @param plugin The plugin
   * @param task The task to run
   * @param delayTicks Initial delay in ticks
   * @param periodTicks Period between executions in ticks
   * @return A cancellable task wrapper
   */
  public static CancellableTask runAsyncTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
    if (IS_FOLIA) {
      // Convert ticks to milliseconds (1 tick = 50ms)
      long delayMs = delayTicks * 50;
      long periodMs = periodTicks * 50;
      var scheduled = Bukkit.getAsyncScheduler().runAtFixedRate(
          plugin, scheduledTask -> task.run(), delayMs, periodMs, TimeUnit.MILLISECONDS
      );
      return scheduled::cancel;
    } else {
      var bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
      return bukkitTask::cancel;
    }
  }

  /**
   * Schedule a repeating sync task on the global region.
   *
   * @param plugin The plugin
   * @param task The task to run
   * @param delayTicks Initial delay in ticks
   * @param periodTicks Period between executions in ticks
   * @return A cancellable task wrapper
   */
  public static CancellableTask runSyncTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
    if (IS_FOLIA) {
      var scheduled = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
          plugin, scheduledTask -> task.run(), delayTicks, periodTicks
      );
      return scheduled::cancel;
    } else {
      var bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
      return bukkitTask::cancel;
    }
  }

  /**
   * Interface for cancellable tasks to abstract between Bukkit and Folia
   */
  @FunctionalInterface
  public interface CancellableTask {

    /**
     * Cancel the task
     */
    void cancel();
  }
}
