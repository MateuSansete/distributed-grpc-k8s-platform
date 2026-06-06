package br.unb.pspd.gateway.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record MoneyDto(
        String currency, 
        @JsonAlias("amount_cents") long amountCents) {
}