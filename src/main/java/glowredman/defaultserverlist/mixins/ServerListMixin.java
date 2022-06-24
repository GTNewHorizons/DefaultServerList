package glowredman.defaultserverlist.mixins;

import glowredman.defaultserverlist.Config;
import glowredman.defaultserverlist.Config.ConfigObj;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerList.class)
public class ServerListMixin {

    @SuppressWarnings("rawtypes")
    @Shadow
    @Final
    private List servers;

    /**
     * Removes all servers from servers.dat that are already in the default list
     * @author glowredman
     */
    @SuppressWarnings("unchecked")
    @Inject(at = @At("TAIL"), method = "loadServerList()V")
    private void removeDuplicateServers(CallbackInfo ci) {
        servers.removeIf(o -> {
            String s1 = ((ServerData) o)
                    .serverIP
                    .replace("http://", "")
                    .replace("https://", "")
                    .replace(":25565", "");
            for (ServerData s2 : Config.SERVERS) {
                if (s1.equals(s2.serverIP
                        .replace("http://", "")
                        .replace("https://", "")
                        .replace(":25565", ""))) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Save default servers
     * @author glowredman
     */
    @Inject(at = @At("TAIL"), method = "saveServerList()V")
    private void saveDefaultServerList(CallbackInfo ci) {
        if (Config.allowDeletions) {
            try {
                Map<String, String> map = new HashMap<>();
                Config.SERVERS.forEach(sd -> map.put(sd.serverName, sd.serverIP));
                ConfigObj newConfig = new ConfigObj(map);
                newConfig.allowDeletions = Config.allowDeletions;
                newConfig.url = Config.url;
                Config.saveConfig(newConfig);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets the ServerData instance stored for the given index in the list.
     * @reason DefaultServerList
     * @author glowredman
     */
    @Overwrite
    public ServerData getServerData(int index) {
        if (index < servers.size()) {
            return (ServerData) servers.get(index);
        }
        return Config.SERVERS.get(index - servers.size());
    }

    /**
     * Removes the ServerData instance stored for the given index in the list.
     * @reason DefaultServerList
     * @author glowredman
     */
    @Overwrite
    public void removeServerData(int index) {
        if (index < servers.size()) {
            servers.remove(index);
        } else if (Config.allowDeletions) {
            Config.SERVERS.remove(index - servers.size());
        }
    }

    /**
     * Counts the number of ServerData instances in the list.
     * @reason DefaultServerList
     * @author glowredman
     */
    @Overwrite
    public int countServers() {
        return servers.size() + Config.SERVERS.size();
    }

    /**
     * Cancel the swap if any of the ServerData objects to swap are in the default server list
     * @author glowredman
     */
    @Inject(at = @At("HEAD"), cancellable = true, method = "swapServers(II)V")
    private void swapServersCheck(int index1, int index2, CallbackInfo ci) {
        if (index1 >= servers.size() || index2 >= servers.size()) {
            ci.cancel();
        }
    }

    /**
     * Sets the ServerData instance stored for the given index in the list.
     * @reason DefaultServerList
     * @author glowredman
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    public void func_147413_a(int index, ServerData data) {
        if (index < servers.size()) {
            servers.set(index, data);
        }
    }
}
