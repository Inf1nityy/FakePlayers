package me.infinityy.fakeplayers;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.infinityy.fakeplayers.command.PlayerCommand;
import me.infinityy.fakeplayers.listener.FakePlayerListener;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
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

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new FakePlayerListener(), this);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }
}
