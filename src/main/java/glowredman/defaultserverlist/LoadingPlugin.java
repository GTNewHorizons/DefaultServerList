package glowredman.defaultserverlist;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.Name;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;
import cpw.mods.fml.relauncher.Side;

@MCVersion("1.7.10")
@Name("DefaultServerList")
@TransformerExclusions("glowredman.defaultserverlist.LoadingPlugin")
public class LoadingPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {

    public static final Logger LOGGER = LogManager.getLogger("DefaultServerList");

    @Override
    public String getMixinConfig() {
        return "mixins.defaultserverlist.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        if (FMLLaunchHandler.side() == Side.CLIENT) {
            return Collections.singletonList("ServerListMixin");
        }
        return Collections.emptyList();
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
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
        if (FMLLaunchHandler.side() == Side.CLIENT) {
            Config.init(new File((File) data.get("mcLocation"), "config"));
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
