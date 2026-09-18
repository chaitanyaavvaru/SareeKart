package com.sareekart.service;

import com.sareekart.dto.trousseau.TrousseauAiCurationRequest;
import com.sareekart.dto.trousseau.TrousseauAiCurationResponse;

public interface TrousseauAiCurationService {

    TrousseauAiCurationResponse curateCeremonyEnsemble(
            Long userId,
            Long boardId,
            Long ceremonyId,
            TrousseauAiCurationRequest request
    );

    String sanitizePromptInput(String input);
}
