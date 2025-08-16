package za.co.titandynamix.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.titandynamix.client.LedgerStatusClient;
import za.co.titandynamix.dto.TransferRequest;
import za.co.titandynamix.entity.Transfer;
import za.co.titandynamix.entity.TransferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentTransferService Tests")
class IPaymentTransferServiceTest {

    @Mock
    private TransferProcessor transferProcessor;

    @Mock
    private LedgerStatusClient ledgerStatusClient;

    @InjectMocks
    private IPaymentTransferService paymentTransferService;

    private TransferRequest transferRequest;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        transferRequest = new TransferRequest(1L, 2L, new BigDecimal("100.00"));
        idempotencyKey = "test-idempotency-key-12345";
    }

    @Test
    @DisplayName("Should create new transfer successfully with valid request")
    void createTransfer_ValidRequest_ShouldReturnTransfer() {
        // Given
        Transfer expectedTransfer = createTestTransfer();
        when(transferProcessor.processSingle(transferRequest, idempotencyKey)).thenReturn(expectedTransfer);

        // When
        Transfer result = paymentTransferService.getTransferStatus(transferRequest, idempotencyKey);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(expectedTransfer.getId());
        assertThat(result.getFromAccountId()).isEqualTo(1L);
        assertThat(result.getToAccountId()).isEqualTo(2L);
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.getStatus()).isEqualTo(TransferStatus.SUCCEEDED);
        assertThat(result.getIdempotencyKey()).isEqualTo(idempotencyKey);

        verify(transferProcessor).processSingle(transferRequest, idempotencyKey);
    }

    @Test
    @DisplayName("Should throw exception when idempotency key is null")
    void getTransferStatus_NullIdempotencyKey_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> paymentTransferService.getTransferStatus(transferRequest, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing required Idempotency-Key header");

        verify(transferProcessor, never()).processSingle(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when idempotency key is blank")
    void getTransferStatus_BlankIdempotencyKey_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> paymentTransferService.getTransferStatus(transferRequest, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing required Idempotency-Key header");

        verify(transferProcessor, never()).processSingle(any(), any());
    }

    // Tests for getTransferStatus(UUID id) method
    @Test
    @DisplayName("Should return transfer when valid ID is provided")
    void getTransferStatus_ValidId_ShouldReturnTransfer() {
        // Given
        UUID transferId = UUID.randomUUID();
        LedgerStatusClient.TransferStatusResponse ledgerResponse = 
                new LedgerStatusClient.TransferStatusResponse(transferId, "SUCCEEDED", "Transfer completed successfully");
        when(ledgerStatusClient.getTransferStatus(transferId)).thenReturn(ledgerResponse);

        // When
        Transfer result = paymentTransferService.getTransferStatus(transferId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(transferId);
        assertThat(result.getStatus()).isEqualTo(TransferStatus.SUCCEEDED);
        assertThat(result.getFailureReason()).isNull();

        verify(ledgerStatusClient).getTransferStatus(transferId);
    }

    @Test
    @DisplayName("Should throw exception when transfer ID is null")
    void getTransferStatus_NullId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> paymentTransferService.getTransferStatus(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transfer ID cannot be null");

        verify(ledgerStatusClient, never()).getTransferStatus(any());
    }

    @Test
    @DisplayName("Should throw exception when transfer not found")
    void getTransferStatus_TransferNotFound_ShouldThrowException() {
        // Given
        UUID transferId = UUID.randomUUID();
        when(ledgerStatusClient.getTransferStatus(transferId))
                .thenThrow(new IllegalArgumentException("Transfer not found: " + transferId));

        // When & Then
        assertThatThrownBy(() -> paymentTransferService.getTransferStatus(transferId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transfer not found: " + transferId);

        verify(ledgerStatusClient).getTransferStatus(transferId);
    }

    @Test
    @DisplayName("Should return transfer with different statuses correctly")
    void getTransferStatus_DifferentStatuses_ShouldReturnCorrectly() {
        // Test PENDING status (default for unknown statuses)
        UUID pendingId = UUID.randomUUID();
        LedgerStatusClient.TransferStatusResponse pendingResponse = 
                new LedgerStatusClient.TransferStatusResponse(pendingId, "PENDING", "Transfer is pending");
        when(ledgerStatusClient.getTransferStatus(pendingId)).thenReturn(pendingResponse);

        Transfer pendingResult = paymentTransferService.getTransferStatus(pendingId);
        assertThat(pendingResult.getStatus()).isEqualTo(TransferStatus.PENDING);
        assertThat(pendingResult.getFailureReason()).isNull();

        // Test FAILED status (mapped from various error states)
        UUID failedId = UUID.randomUUID();
        LedgerStatusClient.TransferStatusResponse failedResponse = 
                new LedgerStatusClient.TransferStatusResponse(failedId, "ERROR", "Transfer failed due to insufficient funds");
        when(ledgerStatusClient.getTransferStatus(failedId)).thenReturn(failedResponse);

        Transfer failedResult = paymentTransferService.getTransferStatus(failedId);
        assertThat(failedResult.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(failedResult.getFailureReason()).isEqualTo("Transfer failed due to insufficient funds");
    }

    @Test
    @DisplayName("Should map ledger statuses correctly")
    void getTransferStatus_StatusMapping_ShouldMapCorrectly() {
        UUID transferId = UUID.randomUUID();
        
        // Test SUCCEEDED mapping
        LedgerStatusClient.TransferStatusResponse succeededResponse = 
                new LedgerStatusClient.TransferStatusResponse(transferId, "SUCCEEDED", "Transfer completed");
        when(ledgerStatusClient.getTransferStatus(transferId)).thenReturn(succeededResponse);
        
        Transfer succeededResult = paymentTransferService.getTransferStatus(transferId);
        assertThat(succeededResult.getStatus()).isEqualTo(TransferStatus.SUCCEEDED);
        assertThat(succeededResult.getFailureReason()).isNull();
        
        // Test NOT_FOUND mapping to FAILED
        LedgerStatusClient.TransferStatusResponse notFoundResponse = 
                new LedgerStatusClient.TransferStatusResponse(transferId, "NOT_FOUND", "Transfer not found");
        when(ledgerStatusClient.getTransferStatus(transferId)).thenReturn(notFoundResponse);
        
        Transfer notFoundResult = paymentTransferService.getTransferStatus(transferId);
        assertThat(notFoundResult.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(notFoundResult.getFailureReason()).isEqualTo("Transfer not found");
        
        // Test unknown status mapping to PENDING
        LedgerStatusClient.TransferStatusResponse unknownResponse = 
                new LedgerStatusClient.TransferStatusResponse(transferId, "PROCESSING", "Transfer is being processed");
        when(ledgerStatusClient.getTransferStatus(transferId)).thenReturn(unknownResponse);
        
        Transfer unknownResult = paymentTransferService.getTransferStatus(transferId);
        assertThat(unknownResult.getStatus()).isEqualTo(TransferStatus.PENDING);
        assertThat(unknownResult.getFailureReason()).isNull();
    }

    private Transfer createTestTransfer() {
        return createTestTransferWithId(UUID.randomUUID());
    }

    private Transfer createTestTransferWithId(UUID id) {
        return Transfer.builder()
                .id(id)
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .status(TransferStatus.SUCCEEDED)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}