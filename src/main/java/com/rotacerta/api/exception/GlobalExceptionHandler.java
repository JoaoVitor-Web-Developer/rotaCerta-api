package com.rotacerta.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;


@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MelhorEnvioApiException.class)
	public ResponseEntity<ApiErrorResponse> handleMelhorEnvioApiException(
			MelhorEnvioApiException ex,
			HttpServletRequest request
	                                                                     ) {
		log.error("Erro na API do Melhor Envio: {}", ex.getMessage());

		ApiErrorResponse errorResponse = new ApiErrorResponse(
				LocalDateTime.now(),
				HttpStatus.FAILED_DEPENDENCY.value(),
				"Failed Dependency",
				"Houve um problema ao se comunicar com o serviço de fretes. Detalhe: " + ex.getMessage(),
				request.getRequestURI()
		);

		return new ResponseEntity<>(errorResponse, HttpStatus.FAILED_DEPENDENCY);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleGenericException(
			Exception ex,
			HttpServletRequest request
	                                                              ) {
		log.error("Erro inesperado na aplicação: {}", ex.getMessage(), ex);

		ApiErrorResponse errorResponse = new ApiErrorResponse(
				LocalDateTime.now(),
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"Internal Server Error",
				"Ocorreu um erro inesperado. Por favor, tente novamente mais tarde.",
				request.getRequestURI()
		);

		return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
