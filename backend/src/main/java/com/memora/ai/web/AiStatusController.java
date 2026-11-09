package com.memora.ai.web;

import com.memora.ai.config.AiProperties;
import com.memora.ai.web.dto.AiStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI", description = "Optional AI-assisted content generation")
public class AiStatusController {

    private final AiProperties aiProperties;

    public AiStatusController(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @GetMapping("/status")
    @Operation(
            summary = "Report whether AI features are available",
            description = """
                    AI is optional: every learning feature works with it switched off.
                    Clients call this to decide whether to show generation actions.
                    """)
    public AiStatusResponse status() {
        return AiStatusResponse.from(aiProperties);
    }
}
