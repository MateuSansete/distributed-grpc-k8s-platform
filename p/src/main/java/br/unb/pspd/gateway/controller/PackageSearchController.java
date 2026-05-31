package br.unb.pspd.gateway.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.unb.pspd.gateway.dto.PackageSearchResponseDto;
import br.unb.pspd.gateway.service.PackageSearchService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** REST API de busca de pacotes de viagem. */
@RestController
@RequestMapping("/api/packages")
@Validated
public class PackageSearchController {

    public static final String SEARCH_TIME_HEADER = "X-Search-Time-Ms";

    private final PackageSearchService service;

    public PackageSearchController(PackageSearchService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public ResponseEntity<PackageSearchResponseDto> search(
            @RequestParam @NotBlank String origin,
            @RequestParam @NotBlank String destination,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate,
            @RequestParam @Min(1) int travelers,
            @RequestParam(required = false, defaultValue = "0") int maxResults) {

        PackageSearchResponseDto response = service.search(
                origin, destination, departureDate, returnDate, travelers, maxResults);

        return ResponseEntity.ok()
                .header(SEARCH_TIME_HEADER, String.valueOf(response.searchTimeMs()))
                .body(response);
    }
}
