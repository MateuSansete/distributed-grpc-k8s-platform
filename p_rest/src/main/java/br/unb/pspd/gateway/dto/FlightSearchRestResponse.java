package br.unb.pspd.gateway.dto;
import java.util.List;
public record FlightSearchRestResponse(List<FlightDto> flights, int total_found) {}