package com.example.buildbot.command;

import com.example.buildbot.BuildBotMod;
import com.example.buildbot.build.BuildTask;
import com.example.buildbot.build.BotManager;
import com.example.buildbot.llm.LLMBuildPlannerService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;

public final class BuildBotCommand {
    private BuildBotCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            CommandManager.literal("buildbot")
                .then(CommandManager.argument("prompt", StringArgumentType.greedyString())
                    .executes(BuildBotCommand::runBuildBotCommand))
        ));
    }

    private static int runBuildBotCommand(final CommandContext<ServerCommandSource> commandContext) {
        final ServerCommandSource serverCommandSource = commandContext.getSource();
        final ServerPlayerEntity serverPlayerEntity;
        try {
            serverPlayerEntity = serverCommandSource.getPlayerOrThrow();
        } catch (final Exception exception) {
            serverCommandSource.sendError(Text.literal("Only players can use /buildbot."));
            return 0;
        }

        final String playerPrompt = StringArgumentType.getString(commandContext, "prompt");
        if (playerPrompt == null || playerPrompt.isBlank()) {
            serverPlayerEntity.sendMessage(Text.literal("Prompt cannot be empty."), false);
            return 0;
        }

        final BuildBotMod buildBotMod = BuildBotMod.getInstance();
        if (buildBotMod == null) {
            serverPlayerEntity.sendMessage(Text.literal("Mod instance is unavailable."), false);
            return 0;
        }

        final ServerWorld serverWorld = serverPlayerEntity.getServerWorld();
        final BlockPos originBlockPos = serverPlayerEntity.getBlockPos();
        final Vec3d botSpawnPosition = Vec3d.ofCenter(originBlockPos.add(1, 0, 1));

        final BotManager botManager = buildBotMod.getBotManager();
        final UUID botUniqueIdentifier = botManager.spawnBotMarker(serverWorld, botSpawnPosition, serverPlayerEntity.getUuid());
        if (botUniqueIdentifier == null) {
            serverPlayerEntity.sendMessage(Text.literal("Failed to spawn build bot."), false);
            return 0;
        }

        serverPlayerEntity.sendMessage(Text.literal("Build bot spawned. Generating build plan..."), false);

        final LLMBuildPlannerService llmBuildPlannerService = buildBotMod.getLlmBuildPlannerService();
        llmBuildPlannerService.createBuildPlanAsync(playerPrompt).whenComplete((instructionList, throwable) -> {
            if (throwable != null) {
                BuildBotMod.LOGGER.error("Failed to generate build plan", throwable);
                serverPlayerEntity.sendMessage(Text.literal("Failed to generate plan from LLM."), false);
                return;
            }

            if (instructionList == null || instructionList.isEmpty()) {
                serverPlayerEntity.sendMessage(Text.literal("LLM returned no build instructions."), false);
                return;
            }

            final BuildTask buildTask = new BuildTask(botUniqueIdentifier, serverWorld.getRegistryKey(), originBlockPos, instructionList);
            botManager.queueBuildTask(buildTask);
            serverPlayerEntity.sendMessage(Text.literal("Build plan received: " + instructionList.size() + " blocks queued."), false);
        });

        return 1;
    }
}
