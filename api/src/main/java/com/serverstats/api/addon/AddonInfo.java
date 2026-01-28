package com.serverstats.api.addon;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define metadata for a ServerStats addon.
 * This annotation must be present on the main addon class.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @AddonInfo(
 *     id = "shopguiplus",
 *     name = "ShopGUI+ Integration",
 *     version = "1.0.0",
 *     author = "YourName",
 *     description = "Tracks ShopGUI+ purchase events"
 * )
 * public class ShopGuiPlusAddon implements Addon {
 *     // ...
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AddonInfo {

    /**
     * Unique identifier for this addon.
     * Should be lowercase with no spaces (e.g., "shopguiplus", "vault-economy").
     *
     * @return The addon ID
     */
    String id();

    /**
     * Human-readable display name for this addon.
     *
     * @return The addon name
     */
    String name();

    /**
     * Version string for this addon.
     *
     * @return The addon version
     */
    String version();

    /**
     * Author of this addon.
     *
     * @return The author name
     */
    String author() default "";

    /**
     * Description of what this addon does.
     *
     * @return The addon description
     */
    String description() default "";

    /**
     * Array of addon IDs that this addon depends on.
     * These addons will be loaded before this one.
     *
     * @return Array of dependency addon IDs
     */
    String[] dependencies() default {};

    /**
     * Array of addon IDs that this addon can optionally use.
     * If present, these addons will be loaded before this one.
     *
     * @return Array of soft dependency addon IDs
     */
    String[] softDependencies() default {};
}
