package glowredman.defaultserverlist;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.VersionParser;
import cpw.mods.fml.common.versioning.VersionRange;

public class ModContainer extends DummyModContainer {

    public ModContainer() {
        super(new ModMetadata());
        ModMetadata md = this.getMetadata();

        // NOTE: If you change this, change mcmod.info too!
        md.authorList = Collections.singletonList("glowredman");
        md.description = "Add default servers to the multiplayer screen.";
        md.modId = "defaultserverlist";
        md.name = "DefaultServerList";
        md.updateUrl = "https://files.data-hole.de/mods/defaultserverlist/updates.json";
        md.url = "https://github.com/glowredman/DefaultServerList";
        md.version = Tags.VERSION;
    }

    @Override
    public VersionRange acceptableMinecraftVersionRange() {
        return VersionParser.parseRange("[1.7.10]");
    }

    @Override
    public Set<ArtifactVersion> getRequirements() {
        return Collections.singleton(VersionParser.parseVersionReference("gtnhmixins"));
    }

    @Override
    public File getSource() {
        return LoadingPlugin.location;
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }

    @Subscribe
    public void preInit(FMLPreInitializationEvent event) {
        Config.init(event.getModConfigurationDirectory());
    }
}
