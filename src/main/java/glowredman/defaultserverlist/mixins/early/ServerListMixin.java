package glowredman.defaultserverlist.mixins.early;

import java.util.List;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import glowredman.defaultserverlist.Config;

@Mixin(ServerList.class)
public abstract class ServerListMixin {

    @Shadow
    @Final
    private List<ServerData> servers;

    @Shadow
    public abstract void saveServerList();

    /**
     * Removes all servers from servers.dat that are already in the default list
     * 
     * @author glowredman
     */
    @Inject(at = @At("TAIL"), method = "loadServerList()V")
    private void removeDuplicateServers(CallbackInfo ci) {
        servers.removeIf(o -> {
            String s1 = o.serverIP.replace("http://", "").replace("https://", "").replace(":25565", "");
            for (ServerData s2 : Config.SERVERS) {
                if (s1.equals(s2.serverIP.replace("http://", "").replace("https://", "").replace(":25565", ""))) {
                    return true;
                }
            }
            return false;
        });
        for (ServerData s : Config.SERVERS) s.field_78841_f = false;
    }

    /**
     * Save default servers
     * 
     * @author glowredman
     */
    @Inject(at = @At("TAIL"), method = "saveServerList()V")
    private void saveDefaultServerList(CallbackInfo ci) {
        if (Config.allowModifications) {
            String[] newServers = new String[Config.SERVERS.size()];
            for (int i = 0; i < newServers.length; i++) {
                ServerData data = Config.SERVERS.get(i);
                newServers[i] = data.serverIP + '|' + data.serverName;
            }
            Config.saveServers(newServers);
        }
    }

    /**
     * Gets the ServerData instance stored for the given index in the list.
     * 
     * @reason DefaultServerList
     * @author glowredman
     */
    @Overwrite
    public ServerData getServerData(int index) {
        if (index < servers.size()) {
            return servers.get(index);
        }
        return Config.SERVERS.get(index - servers.size());
    }

    /**
     * Removes the ServerData instance stored for the given index in the list.
     * 
     * @reason DefaultServerList
     * @author glowredman
     */
    @Overwrite
    public void removeServerData(int index) {
        if (index < servers.size()) {
            servers.remove(index);
        } else if (Config.allowModifications) {
            Config.SERVERS.remove(index - servers.size());
        }
    }

    /**
     * Counts the number of ServerData instances in the list.
     * 
     * @reason DefaultServerList
     * @author glowredman
     */
    @Overwrite
    public int countServers() {
        return servers.size() + Config.SERVERS.size();
    }

    /**
     * Swaps default servers.
     * 
     * @author Quarri6343
     * @reason DefaultServerList
     */
    @Overwrite
    public void swapServers(int index1, int index2) {
        if (index1 < servers.size() && index2 < servers.size()) {
            ServerData serverdata = this.getServerData(index1);
            this.servers.set(index1, this.getServerData(index2));
            this.servers.set(index2, serverdata);
            this.saveServerList();
        } else if (index1 >= servers.size() && index2 >= servers.size()) {
            ServerData serverdata = this.getServerData(index1);
            Config.SERVERS.set(index1 - servers.size(), this.getServerData(index2));
            Config.SERVERS.set(index2 - servers.size(), serverdata);
            this.saveDefaultServerList(null);
        }
    }

    /**
     * Sets the ServerData instance stored for the given index in the list.
     * 
     * @reason DefaultServerList
     * @author glowredman
     */
    @Overwrite
    public void func_147413_a(int index, ServerData data) {
        if (index < servers.size()) {
            servers.set(index, data);
        }
    }
}
