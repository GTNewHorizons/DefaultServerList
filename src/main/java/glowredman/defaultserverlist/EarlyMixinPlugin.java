package glowredman.defaultserverlist;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
public class EarlyMixinPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.defaultserverlist.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        if (FMLLaunchHandler.side().isClient()) {
            return Lists.newArrayList("ServerListMixin", "GuiMultiplayerMixin");
        }
        return Collections.emptyList();
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
