package glowredman.defaultserverlist;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
        acceptedMinecraftVersions = "[1.7.10]",
        dependencies = "required-after:spongemixins",
        modid = "defaultserverlist",
        name = "DefaultServerList",
        version = "GRADLETOKEN_VERSION")
public class ModContainer {

    @EventHandler
    public static void preInit(FMLPreInitializationEvent event) {
        Config.preInit(event.getModConfigurationDirectory());
    }
}
