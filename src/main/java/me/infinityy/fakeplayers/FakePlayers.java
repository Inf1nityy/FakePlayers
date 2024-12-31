package me.infinityy.fakeplayers;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.infinityy.fakeplayers.command.PlayerCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class FakePlayers extends JavaPlugin {

    @Override
    public void onEnable() {
        LifecycleEventManager<@NotNull Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            new PlayerCommand().register(commands);
        });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
