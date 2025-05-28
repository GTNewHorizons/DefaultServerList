package glowredman.defaultserverlist;

import static net.minecraftforge.common.config.Configuration.CATEGORY_GENERAL;

import java.io.File;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.common.config.Configuration;

import org.apache.commons.io.IOUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class Config {

    public static final List<ServerData> SERVERS = new ArrayList<>();
    public static boolean allowModifications;
    private static Configuration config;

    /*
     * spotless:off
     *
     * NOTE: There are three different representations for server list entries:
     * - String array: The entries are in the format ip|name. This is used for the new configs.
     * - Map<String, String>: The key is the name, the value is the ip. This is used for remote server lists.
     * - List<ServerData>: This is used by Minecraft.
     *
     * spotless:on
     */

    public static void init(File configDir) {

        // Setup
        File configFile = new File(configDir, "defaultserverlist.cfg");
        Path legacyConfigPath = configDir.toPath().resolve("defaultserverlist.json");
        boolean migrate = !configFile.exists() && Files.exists(legacyConfigPath);
        Gson gson = new Gson();
        config = new Configuration(configFile);

        // Migrate to new config file if needed.
        if (migrate) {
            LoadingPlugin.LOGGER.info("Found legacy config, attempting to migrate...");
            try (Reader fileReader = Files.newBufferedReader(legacyConfigPath)) {
                ConfigObj legacyConfig = gson.fromJson(fileReader, ConfigObj.class);
                config.get(CATEGORY_GENERAL, "useURL", false).set(legacyConfig.useURL);
                config.get(CATEGORY_GENERAL, "allowModifications", true).set(legacyConfig.allowModifications);
                config.get(CATEGORY_GENERAL, "url", "").set(legacyConfig.url);
                config.get(CATEGORY_GENERAL, "servers", new String[0]).set(toArray(legacyConfig.servers));
                config.get(CATEGORY_GENERAL, "prevDefaultServers", new String[0])
                        .set(legacyConfig.prevDefaultServers.toArray(new String[0]));
                Files.delete(legacyConfigPath);
                LoadingPlugin.LOGGER.info("Migration successful!");
            } catch (Exception e) {
                LoadingPlugin.LOGGER.error("Migration failed!", e);
            }
        }

        // get config values and convert them to a usable format. This also adds comments to the properties.
        boolean useURL = config.getBoolean(
                "useURL",
                CATEGORY_GENERAL,
                false,
                "Whether or not the default servers should be fetched from a remote location.");
        allowModifications = config.getBoolean(
                "allowModifications",
                CATEGORY_GENERAL,
                true,
                "Whether or not the user should be able to delete, modify or change the order of the default servers.");
        String url = config.getString(
                "url",
                CATEGORY_GENERAL,
                "",
                "The remote location to fetch the default servers from. The returned content must be in JSON format (formatted as a map where the keys are the server names and the values the corresponding ip-adresses).");
        Map<String, String> servers = toMap(
                config.getStringList(
                        "servers",
                        CATEGORY_GENERAL,
                        new String[0],
                        "The default servers. Format: ip|name"));
        String[] prevDefaultServersArray = config
                .getStringList("prevDefaultServers", CATEGORY_GENERAL, new String[0], "DO NOT EDIT!");
        Collection<String> prevDefaultServers = new ArrayList<>(prevDefaultServersArray.length);
        Arrays.stream(prevDefaultServersArray).forEachOrdered(prevDefaultServers::add);

        // Fetch servers from the specified remote location.
        if (useURL) {
            try {
                // servers that are currently at the remote location
                Map<String, String> remoteDefaultServers = gson.fromJson(
                        IOUtils.toString(new URL(url), StandardCharsets.UTF_8),
                        new TypeToken<LinkedHashMap<String, String>>() {

                            private static final long serialVersionUID = -1786059589535074931L;
                        }.getType());

                if (allowModifications) {
                    // servers that were added to the remote location since the last time the list was fetched
                    Map<String, String> diff = new LinkedHashMap<>();

                    // calculate diff
                    for (Map.Entry<String, String> entry : remoteDefaultServers.entrySet()) {
                        String ip = entry.getValue();
                        if (!prevDefaultServers.contains(ip)) {
                            diff.put(entry.getKey(), ip);
                        }
                    }

                    // save if the remote location was updated
                    if (!diff.isEmpty()) {
                        servers.putAll(diff);
                        prevDefaultServers = remoteDefaultServers.values();
                        setStringList("servers", toArray(servers));
                        setStringList("prevDefaultServers", prevDefaultServers.toArray(new String[0]));
                    }

                } else {
                    servers = remoteDefaultServers;
                    setStringList("servers", toArray(servers));
                }
            } catch (Exception e) {
                LoadingPlugin.LOGGER
                        .error("Could not get default server list from {}! Are you connected to the internet?", url, e);
            }
        }

        // save the config if it changed.
        if (config.hasChanged()) {
            config.save();
        }

        // Convert from Map<String, String> to List<ServerData>
        servers.forEach((name, ip) -> SERVERS.add(new ServerData(name, ip)));
    }

    private static String[] toArray(Map<String, String> map) {
        String[] array = new String[map.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            array[i] = entry.getValue() + '|' + entry.getKey();
            i++;
        }
        return array;
    }

    private static Map<String, String> toMap(String[] array) {
        Map<String, String> map = new LinkedHashMap<>(array.length);
        for (String entry : array) {
            String[] parts = entry.split("\\|", 2);
            if (parts.length < 2) {
                LoadingPlugin.LOGGER.warn("Could not parse entry {} because not '|' was found!", entry);
                continue;
            }
            map.put(parts[1], parts[0]);
        }
        return map;
    }

    public static void saveServers(String[] servers) {
        setStringList("servers", servers);
        config.save();
    }

    private static void setStringList(String key, String[] values) {
        // config.get(CATEGORY_GENERAL, key, new String[0]).set(values); resets the comment so we can't use that here
        config.getCategory(CATEGORY_GENERAL).get(key).set(values);
    }

    @Deprecated
    public static final class ConfigObj {

        public boolean useURL = false;
        public boolean allowModifications = true;
        public String url = "";
        public Map<String, String> servers = new LinkedHashMap<>();

        @SerializedName("DO_NOT_EDIT_prevDefaultServers")
        public Collection<String> prevDefaultServers = new ArrayList<>();
    }
}
