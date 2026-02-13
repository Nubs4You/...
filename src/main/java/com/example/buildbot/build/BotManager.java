package com.example.buildbot.build;

import com.example.buildbot.BuildBotMod;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;

@Getter
public final class BotManager {
    private static final int MAX_BLOCKS_PER_TICK = 8;

    private final Queue<BuildTask> buildTaskQueue;
    private final Map<UUID, UUID> botOwners;

    public BotManager() {
        this.buildTaskQueue = new LinkedList<>();
        this.botOwners = new HashMap<>();
    }

    public UUID spawnBotMarker(final ServerWorld serverWorld, final Vec3d spawnPosition, final UUID ownerUniqueIdentifier) {
        if (serverWorld == null || spawnPosition == null || ownerUniqueIdentifier == null) {
            return null;
        }

        final ArmorStandEntity armorStandEntity = new ArmorStandEntity(EntityType.ARMOR_STAND, serverWorld);
        armorStandEntity.setPosition(spawnPosition);
        armorStandEntity.setCustomName(Text.literal("Build Bot"));
        armorStandEntity.setCustomNameVisible(true);
        armorStandEntity.setNoGravity(true);
        armorStandEntity.setInvisible(false);

        final boolean spawned = serverWorld.spawnEntity(armorStandEntity);
        if (!spawned) {
            return null;
        }

        final UUID botUniqueIdentifier = armorStandEntity.getUuid();
        this.botOwners.put(botUniqueIdentifier, ownerUniqueIdentifier);
        return botUniqueIdentifier;
    }

    public void queueBuildTask(final BuildTask buildTask) {
        if (buildTask == null || buildTask.getInstructionQueue() == null || buildTask.getInstructionQueue().isEmpty()) {
            return;
        }
        this.buildTaskQueue.offer(buildTask);
    }

    public void onServerTick(final MinecraftServer minecraftServer) {
        if (minecraftServer == null || this.buildTaskQueue.isEmpty()) {
            return;
        }

        final BuildTask currentBuildTask = this.buildTaskQueue.poll();
        if (currentBuildTask == null) {
            return;
        }

        final RegistryKey<World> registryKey = currentBuildTask.getRegistryKey();
        final ServerWorld serverWorld = minecraftServer.getWorld(registryKey);
        if (serverWorld == null) {
            BuildBotMod.LOGGER.warn("Build task skipped because target world is unavailable.");
            return;
        }

        int blocksPlacedThisTick = 0;
        while (blocksPlacedThisTick < MAX_BLOCKS_PER_TICK && !currentBuildTask.getInstructionQueue().isEmpty()) {
            final BlockPlacementInstruction blockPlacementInstruction = currentBuildTask.getInstructionQueue().poll();
            if (blockPlacementInstruction == null) {
                continue;
            }

            final Identifier blockIdentifier = Identifier.tryParse(blockPlacementInstruction.getBlockId());
            if (blockIdentifier == null) {
                continue;
            }

            final Block block = Registries.BLOCK.get(blockIdentifier);
            if (block == null || Objects.equals(block, net.minecraft.block.Blocks.AIR)) {
                continue;
            }

            final BlockPos targetBlockPos = currentBuildTask.getOriginBlockPos().add(
                blockPlacementInstruction.getOffsetX(),
                blockPlacementInstruction.getOffsetY(),
                blockPlacementInstruction.getOffsetZ()
            );
            final BlockState blockState = block.getDefaultState();
            serverWorld.setBlockState(targetBlockPos, blockState, Block.NOTIFY_LISTENERS);
            blocksPlacedThisTick++;
        }

        if (!currentBuildTask.getInstructionQueue().isEmpty()) {
            this.buildTaskQueue.offer(currentBuildTask);
        }
    }
}
