package glowredman.defaultserverlist;

import java.io.File;
import java.io.IOException;
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

import org.apache.commons.io.IOUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import cpw.mods.fml.common.FMLLog;

public class Config {

    public static ConfigObj config = new ConfigObj();
    public static final List<ServerData> SERVERS = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;

    public static void preInit(File configDir) {
        Reader fileReader = null;
        try {
            configPath = configDir.toPath().resolve("defaultserverlist.json");

            if (!Files.exists(configPath)) {
                saveConfig(config);
            } else {
                fileReader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8);
                config = GSON.fromJson(fileReader, ConfigObj.class);
            }

            if (config.useURL) {
                try {
                    // servers that are currently at the remote location
                    Map<String, String> remoteDefaultServers = GSON.fromJson(
                            IOUtils.toString(new URL(config.url), StandardCharsets.UTF_8),
                            new TypeToken<LinkedHashMap<String, String>>() {

                                private static final long serialVersionUID = -1786059589535074931L;
                            }.getType());

                    if (config.allowModifications) {
                        // servers that were added to the remote location since the last time the list was fetched
                        Map<String, String> diff = new LinkedHashMap<>();

                        // calculate diff
                        remoteDefaultServers.forEach((name, ip) -> {
                            if (!config.prevDefaultServers.contains(ip)) {
                                diff.put(name, ip);
                            }
                        });

                        // save if the remote location was updated
                        if (!diff.isEmpty()) {
                            config.servers.putAll(diff);
                            config.prevDefaultServers = remoteDefaultServers.values();
                            saveConfig(config);
                        }

                    } else {
                        config.servers = remoteDefaultServers;
                        saveConfig(config);
                    }
                } catch (Exception e) {
                    FMLLog.warning(
                            "Could not get default server list from default location! Are you connected to the internet?");
                    e.printStackTrace();
                }
            }

            config.servers.forEach((name, ip) -> SERVERS.add(new ServerData(name, ip)));

        } catch (Exception e) {
            FMLLog.severe("Could not parse default server list!");
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(fileReader);
        }
    }

    public static void saveConfig(ConfigObj config) throws IOException {
        File f = configPath.toFile();
        if (f.exists()) {
            f.delete();
        }
        Files.write(configPath, Arrays.asList(GSON.toJson(config)), StandardCharsets.UTF_8);
    }

    public static final class ConfigObj {

        public boolean useURL = false;
        public boolean allowModifications = true;
        public String url = "";
        public Map<String, String> servers = new LinkedHashMap<>();

        @SerializedName("DO_NOT_EDIT_prevDefaultServers")
        public Collection<String> prevDefaultServers = new ArrayList<>();

        public ConfigObj() {}

        public ConfigObj(Map<String, String> servers) {
            this.servers = servers;
        }
    }
}
