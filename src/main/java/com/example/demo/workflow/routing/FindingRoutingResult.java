package com.example.demo.workflow.routing;

import java.util.List;

public record FindingRoutingResult(
        FindingRoute route,
        List<String> strategyGuidance,
        List<String> missingInputs
) {
    public FindingRoutingResult {
        strategyGuidance = strategyGuidance == null ? List.of() : List.copyOf(strategyGuidance);
        missingInputs = missingInputs == null ? List.of() : List.copyOf(missingInputs);
    }
}
