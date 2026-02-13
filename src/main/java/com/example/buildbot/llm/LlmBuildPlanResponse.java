package com.example.buildbot.llm;

import com.example.buildbot.build.BlockPlacementInstruction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class LlmBuildPlanResponse {
    private List<BlockPlacementInstruction> instructions;
}
