package me.infinityy.fakeplayers.lib;

import com.mojang.authlib.GameProfile;
import me.infinityy.fakeplayers.FakePlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FakeServerPlayer extends ServerPlayer {
    private static final Set<String> spawning = new HashSet<>();
    public int forward = 0;
    public int strafing = 0;
    public boolean sneaking = false;

    public Runnable fixStartingPosition = () -> {};
    public boolean isAShadow;

    public static boolean createFake(String username, MinecraftServer server, Vec3 pos, double yaw, double pitch, ResourceKey<Level> dimensionId, GameType gamemode, boolean flying) {
        ServerLevel worldIn = server.getLevel(dimensionId);
        server.services().nameToIdCache().resolveOfflineUsers(false);
        GameProfile gameprofile;

        UUID uuid = OldUsersConverter.convertMobOwnerIfNecessary(server, username);
        if (uuid == null) {
            server.services().nameToIdCache().resolveOfflineUsers(server.isDedicatedServer() && server.usesAuthentication());
            uuid = UUIDUtil.createOfflinePlayerUUID(username);
        }
        gameprofile = new GameProfile(uuid, username);
        String name = gameprofile.name();
        spawning.add(name);

        fetchGameProfile(server, gameprofile.id()).whenCompleteAsync((p, t) -> {
            spawning.remove(name);
            if (t != null) {
                return;
            }

            GameProfile current;
            if (p.name().isEmpty()) {
                current = gameprofile;
            } else {
                current = p;
            }

            FakeServerPlayer instance = new FakeServerPlayer(server, worldIn, current, ClientInformation.createDefault(), false);
            instance.fixStartingPosition = () -> instance.snapTo(pos.x, pos.y, pos.z, (float) yaw, (float) pitch);
            server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), instance, CommonListenerCookie.createInitial(gameprofile, false));
            loadPlayerData(instance);
            instance.stopRiding();
            instance.teleportTo(worldIn, pos.x, pos.y, pos.z, Set.of(), (float) yaw, (float) pitch, true);
            instance.setHealth(20.0F);
            instance.unsetRemoved();
            instance.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.6F);
            instance.gameMode.changeGameModeForPlayer(gamemode);
            server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(instance, (byte) (instance.yHeadRot * 256 / 360)), dimensionId);//instance.dimension);
            server.getPlayerList().broadcastAll(ClientboundEntityPositionSyncPacket.of(instance), dimensionId);
            instance.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f);
            instance.getAbilities().flying = flying;
        }, server);
        return true;
    }

    private static CompletableFuture<GameProfile> fetchGameProfile(MinecraftServer server, final UUID name) {
        final ResolvableProfile resolvableProfile = ResolvableProfile.createUnresolved(name);
        return resolvableProfile.resolveProfile(server.services().profileResolver());
    }

    private static void loadPlayerData(FakeServerPlayer player)
    {
        try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(player.problemPath(), FakePlayers.LOGGER))
        {
            Optional<ValueInput> optional = player.level().getServer().getPlayerList().loadPlayerData(player.nameAndId()).map((compoundTag) -> TagValueInput.create(scopedCollector, player.registryAccess(), compoundTag));
            optional.ifPresent( valueInput -> {
                player.load(valueInput);
                player.loadAndSpawnEnderPearls(valueInput);
                player.loadAndSpawnParentVehicle(valueInput);
            });
        }
    }

    public static ServerPlayer createShadow(MinecraftServer server, ServerPlayer player) {
        server.getPlayerList().remove(player);
        player.connection.disconnect(Component.translatable("multiplayer.disconnect.duplicate_login"));
        ServerLevel worldIn = player.level();
        GameProfile gameprofile = player.getGameProfile();
        FakeServerPlayer playerShadow = new FakeServerPlayer(server, worldIn, gameprofile, player.clientInformation(), true);
        playerShadow.setChatSession(player.getChatSession());

        FakeClientConnection connection = new FakeClientConnection(PacketFlow.SERVERBOUND);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(player.gameProfile, false);
        server.getPlayerList().placeNewPlayer(connection, playerShadow, cookie);

        playerShadow.setHealth(player.getHealth());
        playerShadow.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        playerShadow.gameMode.changeGameModeForPlayer(player.gameMode.getGameModeForPlayer());
        playerShadow.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.6F);
        playerShadow.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, player.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));

        server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(playerShadow, (byte) (player.yHeadRot * 256 / 360)), playerShadow.level().dimension());
        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, playerShadow));
        playerShadow.getAbilities().flying = player.getAbilities().flying;
        return playerShadow;
    }

    public static FakeServerPlayer respawnFake(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation cli) {
        return new FakeServerPlayer(server, level, profile, cli, false);
    }

    public static boolean isSpawningPlayer(String username) {
        return spawning.contains(username);
    }

    private FakeServerPlayer(MinecraftServer server, ServerLevel worldIn, GameProfile profile, ClientInformation cli, boolean shadow) {
        super(server, worldIn, profile, cli);
        isAShadow = shadow;
    }

    @Override
    public void onEquipItem(final @NotNull EquipmentSlot slot, final @NotNull ItemStack previous, final @NotNull ItemStack stack) {
        if (!isUsingItem()) super.onEquipItem(slot, previous, stack);
    }

    @Override
    public void kill(@NotNull ServerLevel level)
    {
        kill(Component.literal("killed"));
    }

    public void kill(Component reason) {
        shakeOff();

        if (reason.getContents() instanceof TranslatableContents text && text.getKey().equals("multiplayer.disconnect.duplicate_login")) {
            this.connection.onDisconnect(new DisconnectionDetails(reason));
        } else {
            MinecraftServer server = MinecraftServer.getServer();
            server.schedule(new TickTask(server.getTickCount(), () -> this.connection.onDisconnect(new DisconnectionDetails(reason))));
        }
    }

    @Override
    public void tick() {
        float vel = this.isCrouching() ? 0.3F : 1.0F;
        this.zza = this.forward * vel;
        this.xxa = this.strafing * vel;
        if (this.sneaking) this.setPose(Pose.CROUCHING);

        try {
            super.tick();
            this.doTick();
        }
        catch (NullPointerException ignored) {
            // happens with that paper port thingy - not sure what that would fix, but hey
            // the game is not going to crash violently.
        }
    }

    private void shakeOff() {
        if (getVehicle() instanceof Player) stopRiding();
        for (Entity passenger : getIndirectPassengers()) {
            if (passenger instanceof Player) passenger.stopRiding();
        }
    }

    @Override
    public void die(@NotNull DamageSource cause) {
        shakeOff();
        super.die(cause);
        setHealth(20);
        this.foodData = new FoodData();
        kill(this.getCombatTracker().getDeathMessage());
    }

    @Override
    public @NotNull String getIpAddress() {
        return "127.0.0.1";
    }

    @Override
    public boolean allowsListing() {
        return true;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) {
        doCheckFallDamage(0.0, y, 0.0, onGround);
    }

    @Override
    public ServerPlayer teleport(@NotNull TeleportTransition serverLevel) {
        super.teleport(serverLevel);
        if (wonGame) {
            ServerboundClientCommandPacket p = new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN);
            connection.handleClientCommand(p);
        }

        // If above branch was taken, *this* has been removed and replaced, the new instance has been set
        // on 'our' connection (which is now theirs, but we still have a ref).
        if (connection.player.isChangingDimension()) {
            connection.player.hasChangedDimension();
        }
        return connection.player;
    }
}