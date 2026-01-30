package com.serverstats.paper.addon;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Custom classloader for Paper addons that properly delegates class loading
 * to allow addons to access classes from other plugins.
 * 
 * This classloader uses a "child-first" pattern for addon classes but delegates
 * to the server's plugin classloaders for external dependencies.
 */
public class PaperAddonClassLoader extends URLClassLoader {

    private final ClassLoader pluginClassLoader;

    /**
     * Create a new addon classloader
     *
     * @param urls The addon JAR URLs
     * @param pluginClassLoader The parent plugin's classloader
     */
    public PaperAddonClassLoader(URL[] urls, ClassLoader pluginClassLoader) {
        super(urls, pluginClassLoader);
        this.pluginClassLoader = pluginClassLoader;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // Check if already loaded
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }

            // Try to load from addon JAR first (child-first for addon packages)
            try {
                c = findClass(name);
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            } catch (ClassNotFoundException e) {
                // Not in addon JAR, continue
            }

            // Try parent plugin's classloader
            try {
                c = pluginClassLoader.loadClass(name);
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            } catch (ClassNotFoundException e) {
                // Not in plugin classloader, continue
            }

            // Try to find in other plugins' classloaders
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                try {
                    ClassLoader loader = plugin.getClass().getClassLoader();
                    if (loader != pluginClassLoader && loader != this) {
                        c = loader.loadClass(name);
                        if (resolve) {
                            resolveClass(c);
                        }
                        return c;
                    }
                } catch (ClassNotFoundException e) {
                    // Not in this plugin, continue
                }
            }

            // Class not found anywhere
            throw new ClassNotFoundException(name);
        }
    }
}
