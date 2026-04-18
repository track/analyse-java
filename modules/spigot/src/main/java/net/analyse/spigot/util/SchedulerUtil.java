package net.analyse.spigot.util;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

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

  /**
   * Not used; prevents instantiation of this utility class.
   */
  private SchedulerUtil() {
  }

  /**
   * Obtains the global region scheduler instance via reflection (Folia).
   *
   * @return The scheduler object
   * @throws Exception if reflection fails
   */
  private static Object getGlobalRegionSchedulerReflect() throws Exception {
    return Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
  }

  /**
   * Obtains the async scheduler instance via reflection (Folia).
   *
   * @return The scheduler object
   * @throws Exception if reflection fails
   */
  private static Object getAsyncSchedulerReflect() throws Exception {
    return Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
  }

  /**
   * Obtains an entity's region scheduler via reflection (Folia).
   *
   * @param entity The entity
   * @return The entity scheduler object
   * @throws Exception if reflection fails
   */
  private static Object getEntitySchedulerReflect(Entity entity) throws Exception {
    return entity.getClass().getMethod("getScheduler").invoke(entity);
  }

  /**
   * Builds a consumer that ignores the scheduled-task argument and runs the given runnable.
   *
   * @param task The task to run
   * @return Consumer suitable for Folia scheduler method parameters
   */
  private static Consumer<Object> consumingRunnable(Runnable task) {
    return scheduledTask -> task.run();
  }

  /**
   * Returns a cancellable that invokes {@code cancel()} on a Folia scheduled task via reflection.
   *
   * @param scheduled The object returned by Folia's scheduler
   * @return A wrapper that cancels via reflection
   */
  private static CancellableTask newFoliaCancellable(final Object scheduled) {
    return () -> {
      try {
        scheduled.getClass().getMethod("cancel").invoke(scheduled);
      } catch (Exception ignored) {
      }
    };
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
      try {
        Object globalScheduler = getGlobalRegionSchedulerReflect();
        Consumer<Object> consumer = consumingRunnable(task);
        globalScheduler.getClass()
            .getMethod("run", Plugin.class, Consumer.class)
            .invoke(globalScheduler, plugin, consumer);
      } catch (Exception e) {
        Bukkit.getScheduler().runTask(plugin, task);
      }
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
      try {
        Object globalScheduler = getGlobalRegionSchedulerReflect();
        Consumer<Object> consumer = consumingRunnable(task);
        globalScheduler.getClass()
            .getMethod("runDelayed", Plugin.class, Consumer.class, long.class)
            .invoke(globalScheduler, plugin, consumer, delayTicks);
      } catch (Exception e) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
      }
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
      try {
        Object entityScheduler = getEntitySchedulerReflect(entity);
        Consumer<Object> consumer = consumingRunnable(task);
        entityScheduler.getClass()
            .getMethod("run", Plugin.class, Consumer.class, Runnable.class)
            .invoke(entityScheduler, plugin, consumer, retired);
      } catch (Exception e) {
        Bukkit.getScheduler().runTask(plugin, task);
      }
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
      try {
        Object entityScheduler = getEntitySchedulerReflect(entity);
        Consumer<Object> consumer = consumingRunnable(task);
        entityScheduler.getClass()
            .getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class)
            .invoke(entityScheduler, plugin, consumer, retired, delayTicks);
      } catch (Exception e) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
      }
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
      try {
        Object asyncScheduler = getAsyncSchedulerReflect();
        Consumer<Object> consumer = consumingRunnable(task);
        asyncScheduler.getClass()
            .getMethod("runNow", Plugin.class, Consumer.class)
            .invoke(asyncScheduler, plugin, consumer);
      } catch (Exception e) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
      }
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
      try {
        // Convert ticks to milliseconds (1 tick = 50ms)
        long delayMs = delayTicks * 50;
        Object asyncScheduler = getAsyncSchedulerReflect();
        Consumer<Object> consumer = consumingRunnable(task);
        asyncScheduler.getClass()
            .getMethod("runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class)
            .invoke(asyncScheduler, plugin, consumer, delayMs, TimeUnit.MILLISECONDS);
      } catch (Exception e) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
      }
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
      try {
        // Convert ticks to milliseconds (1 tick = 50ms)
        long delayMs = delayTicks * 50;
        long periodMs = periodTicks * 50;
        Object asyncScheduler = getAsyncSchedulerReflect();
        Consumer<Object> consumer = consumingRunnable(task);
        Object scheduled = asyncScheduler.getClass()
            .getMethod(
                "runAtFixedRate",
                Plugin.class,
                Consumer.class,
                long.class,
                long.class,
                TimeUnit.class)
            .invoke(asyncScheduler, plugin, consumer, delayMs, periodMs, TimeUnit.MILLISECONDS);
        return newFoliaCancellable(scheduled);
      } catch (Exception e) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        return bukkitTask::cancel;
      }
    } else {
      BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
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
      try {
        Object globalScheduler = getGlobalRegionSchedulerReflect();
        Consumer<Object> consumer = consumingRunnable(task);
        Object scheduled = globalScheduler.getClass()
            .getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class)
            .invoke(globalScheduler, plugin, consumer, delayTicks, periodTicks);
        return newFoliaCancellable(scheduled);
      } catch (Exception e) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        return bukkitTask::cancel;
      }
    } else {
      BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
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
