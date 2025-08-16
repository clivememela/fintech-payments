package za.co.titandynamix.client.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import za.co.titandynamix.client.LedgerStatusClient;

import java.util.UUID;

/**
 * Implementation of LedgerStatusClient that communicates with the Ledger Service
 * via HTTP to fetch transfer status information.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ILedgerStatusClient implements LedgerStatusClient {

    private final WebClient ledgerWebClient;

    @Override
    public TransferStatusResponse getTransferStatus(UUID transferId) {
        if (transferId == null) {
            throw new IllegalArgumentException("Transfer ID cannot be null");
        }

        try {
            log.debug("Fetching transfer status for ID: {}", transferId);
            
            TransferStatusResponse response = ledgerWebClient.get()
                    .uri("/api/ledger/transfers/{id}", transferId)
                    .retrieve()
                    .bodyToMono(TransferStatusResponse.class)
                    .block();

            log.debug("Received transfer status for ID {}: {}", transferId, response);
            return response;
            
        } catch (WebClientResponseException.NotFound e) {
            log.warn("Transfer not found in Ledger Service: {}", transferId);
            throw new IllegalArgumentException("Transfer not found: " + transferId);
        } catch (WebClientResponseException e) {
            log.error("Error fetching transfer status for ID {}: {} - {}", 
                    transferId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch transfer status: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching transfer status for ID {}: {}", transferId, e.getMessage());
            throw new RuntimeException("Failed to fetch transfer status: " + e.getMessage());
        }
    }
}