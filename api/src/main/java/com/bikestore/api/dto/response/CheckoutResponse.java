package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing the newly created Order ID and Mercado Pago checkout details")
public record CheckoutResponse(

        @Schema(description = "Internal ID of the created Order", example = "15")
        Long orderId,

        @Schema(description = "Mercado Pago Preference ID used to identify the checkout session",
                example = "3226905474-059535ac-abe2-4a30-97be-46cf815c92b6")
        String preferenceId,

        @Schema(description = "URL to redirect the user to the Mercado Pago checkout screen",
                example = "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=3226905474-0595...")
        String initPoint
) {}
