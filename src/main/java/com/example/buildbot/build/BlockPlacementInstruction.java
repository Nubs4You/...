package com.example.buildbot.build;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class BlockPlacementInstruction {
    private int offsetX;
    private int offsetY;
    private int offsetZ;
    private String blockId;
}
