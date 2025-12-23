package glowredman.defaultserverlist;

import static net.minecraftforge.common.config.Configuration.CATEGORY_GENERAL;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.common.config.Configuration;

import org.apache.commons.io.IOUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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

    static void init(File configDir) {

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
        final Map<String, String> servers = toMap(
                config.getStringList(
                        "servers",
                        CATEGORY_GENERAL,
                        new String[0],
                        "The default servers. Format: ip|name"));
        String[] prevDefaultServersArray = config
                .getStringList("prevDefaultServers", CATEGORY_GENERAL, new String[0], "DO NOT EDIT!");
        final Collection<String> prevDefaultServers = new ArrayList<>(prevDefaultServersArray.length);
        Arrays.stream(prevDefaultServersArray).forEachOrdered(prevDefaultServers::add);

        // save the config if it changed.
        if (config.hasChanged()) {
            config.save();
        }

        // Fetch servers from the specified remote location.
        if (useURL) {
            final Map<String, String> remoteDefaultServers = new LinkedHashMap<>();

            new Thread(() -> {
                LoadingPlugin.LOGGER.info("Attempting to load servers from remote location...");
                try {
                    fetchRemoteServers(url, remoteDefaultServers);
                } catch (Exception e) {
                    LoadingPlugin.LOGGER.error(
                            "Could not get default server list from {}! Are you connected to the internet?",
                            url,
                            e);
                    return;
                }

                LoadingPlugin.LOGGER.info("Successfully fetched {} servers from {}", remoteDefaultServers.size(), url);

                // func_152344_a = addScheduledTask
                Minecraft.getMinecraft()
                        .func_152344_a(() -> parseServers(servers, prevDefaultServers, remoteDefaultServers));
            }, "DSL Config Thread").start();
        } else {
            // Convert from Map<String, String> to List<ServerData>
            // This has to be executed even if the servers aren't fetched from a remote server
            servers.forEach(Config::addServer);
        }
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
                LoadingPlugin.LOGGER.warn("Could not parse entry {} because no '|' was found!", entry);
                continue;
            }
            map.put(parts[1], parts[0]);
        }
        return map;
    }

    private static void fetchRemoteServers(String url, Map<String, String> remoteDefaultServers)
            throws JsonSyntaxException, IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        try (InputStream is = connection.getInputStream()) {
            String rawJson = IOUtils.toString(is, StandardCharsets.UTF_8);
            TypeToken<LinkedHashMap<String, String>> typeToken = new TypeToken<>() {

                private static final long serialVersionUID = -1786059589535074931L;
            };
            remoteDefaultServers.putAll(new Gson().fromJson(rawJson, typeToken.getType()));
        }
    }

    private static void parseServers(Map<String, String> servers, Collection<String> prevDefaultServers,
            Map<String, String> remoteDefaultServers) {
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
                prevDefaultServers.clear();
                prevDefaultServers.addAll(remoteDefaultServers.values());
                setStringList("servers", toArray(servers));
                setStringList("prevDefaultServers", prevDefaultServers.toArray(new String[0]));
            }

        } else {
            servers.clear();
            servers.putAll(remoteDefaultServers);
            setStringList("servers", toArray(servers));
        }

        // Convert from Map<String, String> to List<ServerData>
        servers.forEach(Config::addServer);

        // save the config if it changed.
        if (config.hasChanged()) {
            config.save();
        }
    }

    private static void addServer(String name, String ip) {
        SERVERS.add(new ServerData(name, ip));
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
