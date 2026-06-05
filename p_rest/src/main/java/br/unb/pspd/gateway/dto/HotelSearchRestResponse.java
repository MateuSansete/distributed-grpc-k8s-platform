package br.unb.pspd.gateway.dto;
import java.util.List;
public record HotelSearchRestResponse(List<HotelDto> hotels, int total_found) {}