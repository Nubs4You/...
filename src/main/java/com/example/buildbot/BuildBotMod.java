package com.example.buildbot;

import com.example.buildbot.build.BotManager;
import com.example.buildbot.command.BuildBotCommand;
import com.example.buildbot.llm.LLMBuildPlannerService;
import lombok.Getter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public final class BuildBotMod implements ModInitializer {
    public static final String MOD_ID = "buildbot";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Getter
    private static BuildBotMod instance;

    private final BotManager botManager;
    private final LLMBuildPlannerService llmBuildPlannerService;

    public BuildBotMod() {
        instance = this;
        this.botManager = new BotManager();
        this.llmBuildPlannerService = new LLMBuildPlannerService();
    }

    @Override
    public void onInitialize() {
        BuildBotCommand.register();
        ServerTickEvents.END_SERVER_TICK.register(server -> this.botManager.onServerTick(server));
        LOGGER.info("Build Bot Mod initialized.");
    }
}
