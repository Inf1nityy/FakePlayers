package me.infinityy.fakeplayers.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.infinityy.fakeplayers.lib.FakeServerPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.papermc.paper.command.brigadier.Commands.*;

public class PlayerCommand {
    public void register(Commands commands) {
        commands.register(literal("player")
                .requires(commandSourceStack -> commandSourceStack.getSender().isOp())
                        .then(argument("player", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(getPlayerSuggestions(c.getSource()), b))
                                .then(literal("spawn").executes(this::spawn))
                                .then(literal("shadow").executes(this::shadow))
                                .then(literal("dismount").executes(this::dismount))
                                .then(literal("drop").executes(context -> drop(context, false)))
                                .then(literal("dropStack").executes(context -> drop(context, true)))
                                .then(literal("sprint").executes(context -> sprint(context, true)))
                                .then(literal("unsprint").executes(context -> sprint(context, false)))
                                .then(literal("sneak").executes(context -> sneak(context, true)))
                                .then(literal("unsneak").executes(context -> sneak(context, false)))
                                .then(literal("swapHands").executes(this::swapHands))
                                .then(literal("jump").executes(this::jump))
                                .then(literal("attack").executes(this::attack))
                                .then(literal("mount").executes(this::mount))
                                .then(literal("use").executes(this::use))
                                .then(literal("stop").executes(this::stop))
                                .then(literal("hotbar").then(argument("slot", IntegerArgumentType.integer(1, 9))).executes(this::hotbar))
                                .then(literal("look").then(argument("position", Vec3Argument.vec3()).executes(this::look)))
                                .then(literal("move").then(argument("direction", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("forward", "backward", "left", "right"), b))
                                        .executes(this::move)))
                )
                .build());
    }

    private Set<String> getPlayerSuggestions(CommandSourceStack source) {
        Set<String> players = new LinkedHashSet<>(List.of("Steve", "Alex"));
        source.getSender().getServer().getOnlinePlayers().forEach(player -> players.add(player.getName()));
        return players;
    }

    private static ServerPlayer getPlayer(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "player");
        return MinecraftServer.getServer().getPlayerList().getPlayerByName(playerName);
    }

    private int stop(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = getPlayer(context);
        if (player instanceof FakeServerPlayer fakePlayer) {
            fakePlayer.forward = 0;
            fakePlayer.strafing = 0;
            fakePlayer.setSprinting(false);
            fakePlayer.sneaking = false;
        }
        return 0;
    }

    private int move(CommandContext<CommandSourceStack> context) {
        String direction = StringArgumentType.getString(context, "direction");

        if (!(getPlayer(context) instanceof FakeServerPlayer fakePlayer)) return 1;
        switch (direction) {
            case "forward":
                fakePlayer.forward = 1;
                break;
            case "backward":
                fakePlayer.forward = -1;
                break;
            case "left":
                fakePlayer.strafing = 1;
                break;
            case "right":
                fakePlayer.strafing = -1;
                break;
        }

        return 0;
    }

    private int look(CommandContext<CommandSourceStack> context) {
        Vec3 position = context.getArgument("position", Coordinates.class).getPosition((net.minecraft.commands.CommandSourceStack) context.getSource());
        getPlayer(context).lookAt(EntityAnchorArgument.Anchor.EYES, position);
        return 0;
    }

    private int use(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = getPlayer(context);
        player.gameMode.useItem(player, player.level(), player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND);
        return 0;
    }

    private int hotbar(CommandContext<CommandSourceStack> context) {
        Integer slot = context.getArgument("slot", Integer.class);
        ServerPlayer player = getPlayer(context);
        player.getInventory().setSelectedHotbarSlot(slot - 1);
        player.connection.send(new ClientboundSetHeldSlotPacket(slot - 1));
        return 0;
    }

    private int mount(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = getPlayer(context);
        EntityHitResult entity = player.getTargetEntity(120);
        if (entity != null) player.startRiding(entity.getEntity());

        return 0;
    }

    private int attack(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = getPlayer(context);

        int reach = 0;
        AttributeInstance reachAttribute = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (reachAttribute != null) reach = (int) reachAttribute.getValue();

        EntityHitResult entity = player.getTargetEntity(reach);
        if (entity != null) {
            player.attack(entity.getEntity());
            player.swing(InteractionHand.MAIN_HAND);
        }

        return 0;
    }

    private int jump(CommandContext<CommandSourceStack> context) {
        getPlayer(context).jumpFromGround();
        return 0;
    }

    private int swapHands(CommandContext<CommandSourceStack> context) {
        getPlayer(context).handleEntityEvent((byte) 55);
        return 0;
    }

    private int sneak(CommandContext<CommandSourceStack> context, boolean crouch) {
        ServerPlayer player = getPlayer(context);
        if (player instanceof FakeServerPlayer fakePlayer)
            fakePlayer.sneaking = crouch;

        return 0;
    }

    private int sprint(CommandContext<CommandSourceStack> context, boolean sprint) {
        getPlayer(context).setSprinting(sprint);
        return 0;
    }

    private int drop(CommandContext<CommandSourceStack> context, boolean entireStack) {
        getPlayer(context).drop(entireStack);
        return 0;
    }

    private int shadow(CommandContext<CommandSourceStack> context) {
        FakeServerPlayer.createShadow(MinecraftServer.getServer(), getPlayer(context));
        return 0;
    }

    private int spawn(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player sender)) return 1;
        ServerPlayer executor = ((CraftPlayer) sender).getHandle();

        FakeServerPlayer.createFake(StringArgumentType.getString(context, "player"), MinecraftServer.getServer(), executor.position(), 0, 0, executor.level().dimension(), GameType.SURVIVAL, false);
        return 0;
    }

    private int dismount(CommandContext<CommandSourceStack> context) {
        getPlayer(context).stopRiding();
        return 0;
    }
}
