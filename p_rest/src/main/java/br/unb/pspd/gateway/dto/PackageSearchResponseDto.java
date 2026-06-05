package br.unb.pspd.gateway.dto;

import java.util.List;

/** Resposta da busca de pacotes, com métricas para o teste de performance gRPC vs REST. */
public record PackageSearchResponseDto(
        List<TravelPackageDto> packages,
        int totalCombinationsEvaluated,
        long searchTimeMs) {
}
