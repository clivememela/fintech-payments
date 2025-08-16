package za.co.titandynamix.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.titandynamix.dto.TransferRequest;
import za.co.titandynamix.dto.TransferResponse;
import za.co.titandynamix.entity.Transfer;
import za.co.titandynamix.service.PaymentTransferService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for transfer operations with comprehensive concurrency control and idempotency support.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Transfer Operations", description = "APIs for managing payment transfers with idempotency and concurrency control")
class TransferRestController {

    private final PaymentTransferService transferService;
    
    @Operation(
        summary = "Health Check",
        description = "Simple health check endpoint to verify service availability",
        tags = {"Health"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service is healthy",
            content = @Content(mediaType = "text/plain", 
                examples = @ExampleObject(value = "Transfer Service is running")))
    })
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        System.out.println("=== DEBUG: Health check endpoint hit ===");
        return ResponseEntity.ok("Transfer Service is running");
    }

    @Operation(
        summary = "Create Transfer",
        description = """
            Creates a new payment transfer with comprehensive idempotency support.
            
            **Idempotency Features:**
            - Duplicate requests with same Idempotency-Key return the same response
            - No double-charging or duplicate transfers
            - 24-hour TTL for idempotency records
            - Returns 201 Created for new transfers, cached response for duplicates
            
            **Concurrency Control:**
            - Thread-safe operations with concurrent data structures
            - Atomic database transactions via Ledger Service
            - Circuit breaker protection for fault tolerance
            """,
        security = @SecurityRequirement(name = "IdempotencyKey")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Transfer created successfully",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = TransferResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "id": "123e4567-e89b-12d3-a456-426614174000",
                        "status": "SUCCEEDED",
                        "failureReason": null
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                        "error": "Validation failed",
                        "message": "Amount must be positive"
                    }
                    """))),
        @ApiResponse(responseCode = "409", description = "Duplicate request (idempotency key already used)",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                        "id": "123e4567-e89b-12d3-a456-426614174000",
                        "status": "SUCCEEDED",
                        "failureReason": null
                    }
                    """))),
        @ApiResponse(responseCode = "422", description = "Business logic error (e.g., insufficient funds)",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                        "id": "123e4567-e89b-12d3-a456-426614174000",
                        "status": "FAILED",
                        "failureReason": "Insufficient funds in source account"
                    }
                    """))),
        @ApiResponse(responseCode = "503", description = "Service unavailable (circuit breaker open)",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                        "error": "Service Unavailable",
                        "message": "Ledger service is temporarily unavailable"
                    }
                    """)))
    })
    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> createTransfer(
            @Parameter(description = "Unique key to ensure idempotent operations", required = true, 
                example = "transfer-2024-001-abc123")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            
            @Parameter(description = "Transfer request details", required = true)
            @Valid @RequestBody TransferRequest request
    ) {
        System.out.println("=== DEBUG: TransferRestController.createTransfer() ENTRY POINT ===");
        System.out.println("DEBUG: Method called at: " + java.time.LocalDateTime.now());
        System.out.println("DEBUG: Idempotency-Key: " + idempotencyKey);
        System.out.println("DEBUG: Request: " + request);
        System.out.println("DEBUG: Thread: " + Thread.currentThread().getName());
        
        Transfer transfer = transferService.getTransferStatus(request, idempotencyKey);
        TransferResponse body = new TransferResponse(transfer.getId(), transfer.getStatus(), transfer.getFailureReason());
        
        System.out.println("DEBUG: Transfer result: " + transfer);
        System.out.println("=== DEBUG: TransferRestController.createTransfer() EXIT ===");
        
        // Return 201 Created for new transfers
        // Note: The idempotency logic in the service layer handles duplicate detection
        return ResponseEntity
                .created(URI.create("/api/transfers/" + transfer.getId()))
                .body(body);
    }

    @Operation(
        summary = "Get Transfer Status",
        description = """
            Retrieves the current status of a specific transfer by its ID.
            
            **Status Values:**
            - `PENDING`: Transfer is being processed
            - `SUCCEEDED`: Transfer completed successfully
            - `FAILED`: Transfer failed (see failureReason for details)
            - `ERROR`: System error occurred during processing
            
            **Real-time Status:** Always returns the latest status from the Ledger Service.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transfer status retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TransferResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "id": "123e4567-e89b-12d3-a456-426614174000",
                        "status": "SUCCEEDED",
                        "failureReason": null
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "Transfer not found",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                        "error": "Not Found",
                        "message": "Transfer with ID 123e4567-e89b-12d3-a456-426614174000 not found"
                    }
                    """)))
    })
    @GetMapping("/transfers/{id}")
    public ResponseEntity<TransferResponse> getTransferStatus(
            @Parameter(description = "Transfer ID", required = true, 
                example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        try {
            Transfer transfer = transferService.getTransferStatus(id);
            return ResponseEntity.ok(new TransferResponse(transfer.getId(), transfer.getStatus(), transfer.getFailureReason()));
        } catch (IllegalArgumentException e) {
            // Return 404 for transfer not found or invalid ID
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Create Batch Transfers",
        description = """
            Processes multiple transfers concurrently in a batch operation with high-performance parallel processing.
            
            **Concurrency Features:**
            - Parallel processing using CompletableFuture and custom thread pool
            - Thread pool sized based on available processors and batch size
            - Automatic idempotency keys generated for batch items
            - Timeout protection (30 seconds)
            - Individual failure handling (one failure doesn't affect others)
            
            **Performance:**
            - Up to 100 transfers per batch
            - Dynamic thread pool sizing: min(batch_size, processors * 2)
            - Concurrent execution with timeout protection
            
            **Idempotency:** Each transfer in the batch gets a deterministic idempotency key based on batch ID and item index.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Batch processing completed",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TransferResponse.class),
                examples = @ExampleObject(value = """
                    [
                        {
                            "id": "123e4567-e89b-12d3-a456-426614174000",
                            "status": "SUCCEEDED",
                            "failureReason": null
                        },
                        {
                            "id": "123e4567-e89b-12d3-a456-426614174001",
                            "status": "FAILED",
                            "failureReason": "Insufficient funds in source account"
                        }
                    ]
                    """))),
        @ApiResponse(responseCode = "400", description = "Invalid batch request (e.g., too many items)",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                        "error": "Bad Request",
                        "message": "Batch size exceeds maximum limit of 100 transfers"
                    }
                    """))),
        @ApiResponse(responseCode = "408", description = "Batch processing timeout",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                        "error": "Request Timeout",
                        "message": "Batch processing exceeded 30 second timeout"
                    }
                    """)))
    })
    @PostMapping("/transfers/batch")
    public ResponseEntity<List<TransferResponse>> createTransfersBatch(
            @Parameter(description = "List of transfer requests to process (max 100)", required = true)
            @Valid @RequestBody List<TransferRequest> requests
    ) {
        List<Transfer> transfers = transferService.createTransfersBatch(requests);
        List<TransferResponse> body = transfers.stream()
                .map(t -> new TransferResponse(t.getId(), t.getStatus(), t.getFailureReason()))
                .toList();
        return ResponseEntity.ok(body);
    }
}