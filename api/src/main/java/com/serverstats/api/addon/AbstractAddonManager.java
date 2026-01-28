package com.serverstats.api.addon;

import com.serverstats.api.platform.ServerStatsPlatform;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Abstract base implementation of {@link AddonManager} that handles addon loading,
 * dependency resolution, and lifecycle management. Platform implementations extend
 * this class and provide platform-specific logging.
 */
public abstract class AbstractAddonManager implements AddonManager {

    private static final String ADDON_FILE_EXTENSION = ".jar";

    private final ServerStatsPlatform platform;
    private final Path addonsFolder;
    private final Map<String, LoadedAddonImpl> loadedAddons = new LinkedHashMap<>();
    private final Map<String, URLClassLoader> addonClassLoaders = new HashMap<>();

    /**
     * Create a new addon manager
     *
     * @param platform The ServerStats platform instance
     * @param addonsFolder The folder to load addons from
     */
    public AbstractAddonManager(ServerStatsPlatform platform, Path addonsFolder) {
        this.platform = platform;
        this.addonsFolder = addonsFolder;
    }

    @Override
    public void loadAddons() {
        // Create addons folder if it doesn't exist
        if (!Files.exists(addonsFolder)) {
            try {
                Files.createDirectories(addonsFolder);
                logInfo("Created addons folder: " + addonsFolder);
            } catch (IOException e) {
                logError("Failed to create addons folder", e);
                return;
            }
        }

        // Find all JAR files in the addons folder
        File[] jarFiles = addonsFolder.toFile().listFiles((dir, name) -> name.endsWith(ADDON_FILE_EXTENSION));
        if (jarFiles == null || jarFiles.length == 0) {
            logInfo("No addons found in " + addonsFolder);
            return;
        }

        logInfo(String.format("Found %d addon(s) to load", jarFiles.length));

        // Discover addons and their metadata
        Map<String, AddonCandidate> candidates = new HashMap<>();
        for (File jarFile : jarFiles) {
            AddonCandidate candidate = discoverAddon(jarFile.toPath());
            if (candidate != null) {
                candidates.put(candidate.info.id(), candidate);
            }
        }

        // Resolve load order based on dependencies
        List<String> loadOrder = resolveLoadOrder(candidates);

        // Load addons in dependency order
        for (String addonId : loadOrder) {
            AddonCandidate candidate = candidates.get(addonId);
            if (candidate != null) {
                loadAddon(candidate);
            }
        }

        logInfo(String.format("Loaded %d addon(s)", loadedAddons.size()));
    }

    @Override
    public void enableAddons() {
        int enabled = 0;
        for (LoadedAddonImpl loadedAddon : loadedAddons.values()) {
            if (!loadedAddon.isEnabled()) {
                if (enableAddonInternal(loadedAddon)) {
                    enabled++;
                }
            }
        }

        if (enabled > 0) {
            logInfo(String.format("Enabled %d addon(s)", enabled));
        }
    }

    @Override
    public void disableAddons() {
        // Disable in reverse order
        List<LoadedAddonImpl> addonsToDisable = new ArrayList<>(loadedAddons.values());
        for (int i = addonsToDisable.size() - 1; i >= 0; i--) {
            LoadedAddonImpl loadedAddon = addonsToDisable.get(i);
            if (loadedAddon.isEnabled()) {
                disableAddonInternal(loadedAddon);
            }
        }

        // Close all classloaders
        for (URLClassLoader classLoader : addonClassLoaders.values()) {
            try {
                classLoader.close();
            } catch (IOException e) {
                logError("Failed to close classloader", e);
            }
        }

        loadedAddons.clear();
        addonClassLoaders.clear();
    }

    @Override
    public void reloadAddons() {
        logInfo("Reloading all addons...");
        disableAddons();
        loadAddons();
        enableAddons();
    }

    @Override
    public boolean reloadAddon(String addonId) {
        LoadedAddonImpl loadedAddon = loadedAddons.get(addonId);
        if (loadedAddon == null) {
            logWarning("Cannot reload addon '" + addonId + "': not loaded");
            return false;
        }

        Path jarPath = loadedAddon.getJarPath();

        // Disable the addon
        if (loadedAddon.isEnabled()) {
            disableAddonInternal(loadedAddon);
        }

        // Close the classloader
        URLClassLoader classLoader = addonClassLoaders.remove(addonId);
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (IOException e) {
                logError("Failed to close classloader for addon " + addonId, e);
            }
        }

        // Remove from loaded addons
        loadedAddons.remove(addonId);

        // Rediscover and reload
        AddonCandidate candidate = discoverAddon(jarPath);
        if (candidate == null) {
            logError("Failed to rediscover addon from " + jarPath, null);
            return false;
        }

        // Verify it's the same addon
        if (!candidate.info.id().equals(addonId)) {
            logWarning("Addon ID changed from '" + addonId + "' to '" + candidate.info.id() + "'");
        }

        // Load and enable
        if (!loadAddon(candidate)) {
            return false;
        }

        LoadedAddonImpl reloaded = loadedAddons.get(candidate.info.id());
        if (reloaded != null) {
            return enableAddonInternal(reloaded);
        }

        return false;
    }

    @Override
    public boolean enableAddon(String addonId) {
        LoadedAddonImpl loadedAddon = loadedAddons.get(addonId);
        if (loadedAddon == null) {
            logWarning("Cannot enable addon '" + addonId + "': not loaded");
            return false;
        }

        if (loadedAddon.isEnabled()) {
            logWarning("Addon '" + addonId + "' is already enabled");
            return true;
        }

        return enableAddonInternal(loadedAddon);
    }

    @Override
    public boolean disableAddon(String addonId) {
        LoadedAddonImpl loadedAddon = loadedAddons.get(addonId);
        if (loadedAddon == null) {
            logWarning("Cannot disable addon '" + addonId + "': not loaded");
            return false;
        }

        if (!loadedAddon.isEnabled()) {
            logWarning("Addon '" + addonId + "' is already disabled");
            return true;
        }

        return disableAddonInternal(loadedAddon);
    }

    @Override
    public Optional<LoadedAddon> getAddon(String addonId) {
        return Optional.ofNullable(loadedAddons.get(addonId));
    }

    @Override
    public Collection<LoadedAddon> getLoadedAddons() {
        return new ArrayList<>(loadedAddons.values());
    }

    @Override
    public boolean isAddonEnabled(String addonId) {
        LoadedAddonImpl loadedAddon = loadedAddons.get(addonId);
        return loadedAddon != null && loadedAddon.isEnabled();
    }

    @Override
    public boolean isAddonLoaded(String addonId) {
        return loadedAddons.containsKey(addonId);
    }

    /**
     * Discover an addon from a JAR file
     *
     * @param jarPath Path to the JAR file
     * @return AddonCandidate if found, null otherwise
     */
    private AddonCandidate discoverAddon(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            // Create a temporary classloader to scan classes
            URL jarUrl = jarPath.toUri().toURL();
            try (URLClassLoader tempLoader = new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader())) {
                // Scan all classes in the JAR
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.endsWith(".class") && !name.contains("$")) {
                        String className = name.replace('/', '.').replace(".class", "");
                        try {
                            Class<?> clazz = tempLoader.loadClass(className);
                            AddonInfo info = clazz.getAnnotation(AddonInfo.class);
                            if (info != null && Addon.class.isAssignableFrom(clazz)) {
                                logDebug("Discovered addon: " + info.name() + " (" + info.id() + ") v" + info.version());
                                @SuppressWarnings("unchecked")
                                Class<? extends Addon> addonClass = (Class<? extends Addon>) clazz;
                                return new AddonCandidate(jarPath, className, addonClass.getName(), info);
                            }
                        } catch (ClassNotFoundException | NoClassDefFoundError e) {
                            // Class has dependencies we don't have, skip it
                        }
                    }
                }
            }
        } catch (IOException e) {
            logError("Failed to scan addon JAR: " + jarPath, e);
        }

        logWarning("No valid addon class found in " + jarPath.getFileName());
        return null;
    }

    /**
     * Resolve the load order based on dependencies
     *
     * @param candidates Map of addon candidates
     * @return Ordered list of addon IDs to load
     */
    private List<String> resolveLoadOrder(Map<String, AddonCandidate> candidates) {
        List<String> result = new ArrayList<>();
        Set<String> resolved = new HashSet<>();
        Set<String> unresolved = new HashSet<>();

        for (String addonId : candidates.keySet()) {
            resolveDependencies(addonId, candidates, resolved, unresolved, result);
        }

        return result;
    }

    /**
     * Recursively resolve dependencies for an addon
     */
    private void resolveDependencies(String addonId, Map<String, AddonCandidate> candidates,
                                      Set<String> resolved, Set<String> unresolved, List<String> result) {
        if (resolved.contains(addonId)) {
            return;
        }

        if (unresolved.contains(addonId)) {
            logWarning("Circular dependency detected for addon: " + addonId);
            return;
        }

        AddonCandidate candidate = candidates.get(addonId);
        if (candidate == null) {
            return;
        }

        unresolved.add(addonId);

        // Resolve hard dependencies first
        for (String dependency : candidate.info.dependencies()) {
            if (!candidates.containsKey(dependency)) {
                logWarning(String.format("Addon '%s' requires missing dependency '%s'", addonId, dependency));
                unresolved.remove(addonId);
                return;
            }

            resolveDependencies(dependency, candidates, resolved, unresolved, result);
        }

        // Resolve soft dependencies (if present)
        for (String softDep : candidate.info.softDependencies()) {
            if (candidates.containsKey(softDep)) {
                resolveDependencies(softDep, candidates, resolved, unresolved, result);
            }
        }

        unresolved.remove(addonId);
        resolved.add(addonId);
        result.add(addonId);
    }

    /**
     * Load an addon from a candidate
     *
     * @param candidate The addon candidate
     * @return true if loaded successfully
     */
    private boolean loadAddon(AddonCandidate candidate) {
        try {
            // Check for missing dependencies
            for (String dependency : candidate.info.dependencies()) {
                if (!loadedAddons.containsKey(dependency)) {
                    logError("Cannot load addon '" + candidate.info.id() + "': missing dependency '" + dependency + "'", null);
                    return false;
                }
            }

            // Create isolated classloader for this addon
            URL jarUrl = candidate.jarPath.toUri().toURL();
            URLClassLoader addonClassLoader = new URLClassLoader(
                new URL[]{jarUrl},
                getClass().getClassLoader()
            );

            // Load the addon class
            Class<?> addonClass = addonClassLoader.loadClass(candidate.className);
            Addon addon = (Addon) addonClass.getDeclaredConstructor().newInstance();

            // Create data folder
            Path dataFolder = addonsFolder.resolve(candidate.info.id());
            if (!Files.exists(dataFolder)) {
                Files.createDirectories(dataFolder);
            }

            // Create logger
            AddonLogger logger = createAddonLogger(candidate.info.id(), candidate.info.name());

            // Create loaded addon wrapper
            LoadedAddonImpl loadedAddon = new LoadedAddonImpl(
                addon,
                candidate.info,
                candidate.jarPath,
                dataFolder,
                logger
            );

            // Call onLoad
            addon.onLoad(platform, logger, dataFolder);

            // Store
            loadedAddons.put(candidate.info.id(), loadedAddon);
            addonClassLoaders.put(candidate.info.id(), addonClassLoader);

            logDebug("Loaded addon: " + candidate.info.name() + " v" + candidate.info.version());
            return true;
        } catch (Exception e) {
            logError("Failed to load addon '" + candidate.info.id() + "'", e);
            return false;
        }
    }

    /**
     * Enable an addon
     *
     * @param loadedAddon The loaded addon
     * @return true if enabled successfully
     */
    private boolean enableAddonInternal(LoadedAddonImpl loadedAddon) {
        try {
            loadedAddon.getAddon().onEnable();
            loadedAddon.setEnabled(true);
            logInfo("Enabled addon: " + loadedAddon.getName() + " v" + loadedAddon.getVersion());
            return true;
        } catch (Exception e) {
            logError("Failed to enable addon '" + loadedAddon.getId() + "'", e);
            return false;
        }
    }

    /**
     * Disable an addon
     *
     * @param loadedAddon The loaded addon
     * @return true if disabled successfully
     */
    private boolean disableAddonInternal(LoadedAddonImpl loadedAddon) {
        try {
            loadedAddon.getAddon().onDisable();
            loadedAddon.setEnabled(false);
            logDebug("Disabled addon: " + loadedAddon.getName());
            return true;
        } catch (Exception e) {
            logError("Failed to disable addon '" + loadedAddon.getId() + "'", e);
            return false;
        }
    }

    /**
     * Create a logger for an addon (platform-specific)
     *
     * @param addonId The addon ID
     * @param addonName The addon display name
     * @return The addon logger
     */
    protected abstract AddonLogger createAddonLogger(String addonId, String addonName);

    /**
     * Log an info message
     *
     * @param message The message
     */
    protected abstract void logInfo(String message);

    /**
     * Log a warning message
     *
     * @param message The message
     */
    protected abstract void logWarning(String message);

    /**
     * Log an error message
     *
     * @param message The message
     * @param throwable Optional throwable
     */
    protected abstract void logError(String message, Throwable throwable);

    /**
     * Log a debug message
     *
     * @param message The message
     */
    protected abstract void logDebug(String message);

    /**
     * Get the platform instance
     *
     * @return The platform
     */
    protected ServerStatsPlatform getPlatform() {
        return platform;
    }

    /**
     * Internal class to hold addon candidate information during discovery
     */
    private static class AddonCandidate {
        final Path jarPath;
        final String className;
        final String fullClassName;
        final AddonInfo info;

        AddonCandidate(Path jarPath, String className, String fullClassName, AddonInfo info) {
            this.jarPath = jarPath;
            this.className = className;
            this.fullClassName = fullClassName;
            this.info = info;
        }
    }

    /**
     * Implementation of LoadedAddon
     */
    private static class LoadedAddonImpl implements LoadedAddon {
        private final Addon addon;
        private final AddonInfo info;
        private final Path jarPath;
        private final Path dataFolder;
        private final AddonLogger logger;
        private boolean enabled = false;

        LoadedAddonImpl(Addon addon, AddonInfo info, Path jarPath, Path dataFolder, AddonLogger logger) {
            this.addon = addon;
            this.info = info;
            this.jarPath = jarPath;
            this.dataFolder = dataFolder;
            this.logger = logger;
        }

        @Override
        public Addon getAddon() {
            return addon;
        }

        @Override
        public AddonInfo getInfo() {
            return info;
        }

        @Override
        public Path getDataFolder() {
            return dataFolder;
        }

        @Override
        public AddonLogger getLogger() {
            return logger;
        }

        @Override
        public Path getJarPath() {
            return jarPath;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
