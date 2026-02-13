package com.example.buildbot.llm;

import com.example.buildbot.BuildBotMod;
import com.example.buildbot.build.BlockPlacementInstruction;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Getter
@Setter
public final class LLMBuildPlannerService {
    private static final String DEFAULT_ENDPOINT = "http://localhost:11434/api/generate";

    private final HttpClient httpClient;
    private final Gson gson;
    private String llmEndpoint;

    public LLMBuildPlannerService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10L))
            .build();
        this.gson = new Gson();
        this.llmEndpoint = DEFAULT_ENDPOINT;
    }

    public CompletableFuture<List<BlockPlacementInstruction>> createBuildPlanAsync(final String playerPrompt) {
        if (playerPrompt == null || playerPrompt.isBlank()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> this.createBuildPlan(playerPrompt));
    }

    private List<BlockPlacementInstruction> createBuildPlan(final String playerPrompt) {
        final String formattingInstruction = "Return JSON only with shape: {\"instructions\":[{\"offsetX\":0,\"offsetY\":0,\"offsetZ\":0,\"blockId\":\"minecraft:stone\"}]}.";
        final String prompt = formattingInstruction + " Build this structure: " + playerPrompt;

        final Map<String, Object> requestPayload = Map.of(
            "model", "llama3.1",
            "prompt", prompt,
            "stream", false
        );

        final String requestJson = this.gson.toJson(requestPayload);
        final HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(this.llmEndpoint))
            .timeout(Duration.ofSeconds(45L))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
            .build();

        try {
            final HttpResponse<String> httpResponse = this.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                BuildBotMod.LOGGER.warn("LLM service returned non-success status code: {}", httpResponse.statusCode());
                return Collections.emptyList();
            }

            final String rawBody = httpResponse.body();
            if (rawBody == null || rawBody.isBlank()) {
                return Collections.emptyList();
            }

            final String extractedJson = this.extractJsonPayload(rawBody);
            final LlmBuildPlanResponse llmBuildPlanResponse = this.gson.fromJson(extractedJson, LlmBuildPlanResponse.class);
            if (llmBuildPlanResponse == null || llmBuildPlanResponse.getInstructions() == null) {
                return Collections.emptyList();
            }

            return llmBuildPlanResponse.getInstructions();
        } catch (final IOException | InterruptedException | JsonSyntaxException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            BuildBotMod.LOGGER.error("Error while requesting build instructions", exception);
            return Collections.emptyList();
        }
    }

    private String extractJsonPayload(final String rawBody) {
        try {
            final Map<?, ?> responseContainer = this.gson.fromJson(rawBody, Map.class);
            if (responseContainer != null && responseContainer.containsKey("response")) {
                final Object responseObject = responseContainer.get("response");
                if (responseObject instanceof final String responseString && !responseString.isBlank()) {
                    return responseString;
                }
            }
        } catch (final Exception exception) {
            BuildBotMod.LOGGER.debug("Failed to parse wrapped LLM response, using raw body as fallback.");
        }
        return rawBody;
    }
}
