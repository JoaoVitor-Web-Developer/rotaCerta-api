package com.rotacerta.api.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rotacerta.api.model.entities.Quote;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class QuoteHistoryDTO {

	private UUID id;
	private String originZip;
	private String destZip;
	private QuoteRequestDTO.PackageDTO aPackage;
	private List<ShippingOptionDTO> options;
	private OffsetDateTime createdAt;

	public static QuoteHistoryDTO fromEntity(Quote quote, ObjectMapper objectMapper) {
		try {
			QuoteRequestDTO.PackageDTO packageDTO = objectMapper.readValue(quote.getPayload(), QuoteRequestDTO.PackageDTO.class);
			List<ShippingOptionDTO> optionsDTO = objectMapper.readValue(quote.getResult(), new TypeReference<>() {});

			return QuoteHistoryDTO.builder()
			                      .id(quote.getId())
			                      .originZip(quote.getOriginZip())
			                      .destZip(quote.getDestZip())
			                      .aPackage(packageDTO)
			                      .options(optionsDTO)
			                      .createdAt(quote.getCreatedAt())
			                      .build();

		} catch (JsonProcessingException e) {
			return QuoteHistoryDTO.builder()
			                      .id(quote.getId())
			                      .originZip(quote.getOriginZip())
			                      .destZip(quote.getDestZip())
			                      .options(Collections.emptyList())
			                      .createdAt(quote.getCreatedAt())
			                      .build();
		}
	}
}
