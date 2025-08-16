package za.co.titandynamix.client.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import za.co.titandynamix.client.LedgerTransferClient;
import za.co.titandynamix.dto.TransferRequest;
import za.co.titandynamix.entity.Transfer;
import za.co.titandynamix.entity.TransferStatus;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ILedgerTransferClient implements LedgerTransferClient {

    private final WebClient ledgerWebClient;

    @Override
    public Result performTransfer(UUID transferId, Long fromAccountId, Long toAccountId, BigDecimal amount) {
        Map<String, Object> payload = Map.of(
                "transferId", transferId,
                "fromAccountId", fromAccountId,
                "toAccountId", toAccountId,
                "amount", amount
        );

        try {
            ledgerWebClient.post()
                    .uri("/api/ledger/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return new Result(true, "OK");
        } catch (WebClientResponseException webClientResponseException) {
            String msg = webClientResponseException.getResponseBodyAsString();
            return new Result(false, (msg == null || msg.isBlank()) ? webClientResponseException.getStatusText() : msg);
        } catch (Exception ex) {
            return new Result(false, ex.getMessage());
        }
    }

    @Override
    public Transfer createAndProcessTransfer(TransferRequest request, String idempotencyKey) {
        log.debug("Creating and processing transfer: fromAccount={}, toAccount={}, amount={}, idempotencyKey={}", 
                request.fromAccountId(), request.toAccountId(), request.amount(), idempotencyKey);

        Map<String, Object> payload = Map.of(
                "fromAccountId", request.fromAccountId(),
                "toAccountId", request.toAccountId(),
                "amount", request.amount()
        );

        try {
            var webClientBuilder = ledgerWebClient.post()
                    .uri("/api/ledger/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload);

            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                webClientBuilder = webClientBuilder.header("Idempotency-Key", idempotencyKey);
            }

            LedgerTransferResponse response = webClientBuilder
                    .retrieve()
                    .bodyToMono(LedgerTransferResponse.class)
                    .block();

            log.debug("Transfer created successfully: transferId={}, status={}", response.transferId(), response.status());

            return Transfer.builder()
                    .id(response.transferId())
                    .fromAccountId(request.fromAccountId())
                    .toAccountId(request.toAccountId())
                    .amount(request.amount())
                    .status(mapLedgerStatusToTransferStatus(response.status()))
                    .idempotencyKey(idempotencyKey)
                    .failureReason(response.status().equals("FAILED") ? response.message() : null)
                    .build();

        } catch (WebClientResponseException e) {
            log.error("Error creating transfer: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            
            if (e.getStatusCode().value() == 409) {
                throw new IllegalArgumentException("Duplicate idempotency key: " + idempotencyKey);
            }
            
            return Transfer.builder()
                    .id(UUID.randomUUID())
                    .fromAccountId(request.fromAccountId())
                    .toAccountId(request.toAccountId())
                    .amount(request.amount())
                    .status(TransferStatus.FAILED)
                    .idempotencyKey(idempotencyKey)
                    .failureReason("HTTP Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString())
                    .build();
                    
        } catch (Exception e) {
            log.error("Unexpected error creating transfer", e);
            
            return Transfer.builder()
                    .id(UUID.randomUUID())
                    .fromAccountId(request.fromAccountId())
                    .toAccountId(request.toAccountId())
                    .amount(request.amount())
                    .status(TransferStatus.FAILED)
                    .idempotencyKey(idempotencyKey)
                    .failureReason("System Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Response record for transfer creation from Ledger Service
     */
    private record LedgerTransferResponse(UUID transferId, String status, String message) {}

    /**
     * Maps Ledger Service status to Transfer Service status
     */
    private TransferStatus mapLedgerStatusToTransferStatus(String ledgerStatus) {
        return switch (ledgerStatus) {
            case "SUCCEEDED" -> TransferStatus.SUCCEEDED;
            case "FAILED", "ERROR", "INVALID" -> TransferStatus.FAILED;
            default -> TransferStatus.PENDING;
        };
    }
}