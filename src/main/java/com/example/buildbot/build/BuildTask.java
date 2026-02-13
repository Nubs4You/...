package com.example.buildbot.build;

import lombok.Getter;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

@Getter
public final class BuildTask {
    private final UUID botUniqueIdentifier;
    private final RegistryKey<World> registryKey;
    private final BlockPos originBlockPos;
    private final Queue<BlockPlacementInstruction> instructionQueue;

    public BuildTask(final UUID botUniqueIdentifier,
                     final RegistryKey<World> registryKey,
                     final BlockPos originBlockPos,
                     final List<BlockPlacementInstruction> instructionList) {
        this.botUniqueIdentifier = botUniqueIdentifier;
        this.registryKey = registryKey;
        this.originBlockPos = originBlockPos.toImmutable();
        this.instructionQueue = new ArrayDeque<>(instructionList);
    }
}
