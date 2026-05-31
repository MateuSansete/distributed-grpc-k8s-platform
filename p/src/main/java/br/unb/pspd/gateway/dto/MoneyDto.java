package br.unb.pspd.gateway.dto;

import br.unb.pspd.travel.proto.Money;

/** Valor monetário plano para o JSON da REST API. */
public record MoneyDto(String currency, long amountCents) {

    public static MoneyDto from(Money money) {
        return new MoneyDto(money.getCurrency(), money.getAmountCents());
    }
}
