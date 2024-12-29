package me.infinityy.fakeplayers.command;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.infinityy.fakeplayers.lib.FakeServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PlayerCommand {
    public void register(Commands commands) {
        commands.register(Commands.literal("player")
                .requires(commandSourceStack -> commandSourceStack.getSender().isOp())
                        .then(Commands.literal("spawn").executes(this::spawnPlayer))
                .build());
    }

    private int spawnPlayer(CommandContext<CommandSourceStack> commandSourceStackCommandContext) {
        FakeServerPlayer.createFake("SurgeFlame", MinecraftServer.getServer(), new Vec3(0, 100, 0), 0, 0, Level.OVERWORLD, GameType.CREATIVE, false);
        return 0;
    }
}
