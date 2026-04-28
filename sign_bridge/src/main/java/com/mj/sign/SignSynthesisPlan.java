package com.mj.sign;

import java.util.List;

public record SignSynthesisPlan(
        List<String> glosses,
        List<String> non_manual_markers,
        String grammar_note
) {
}
