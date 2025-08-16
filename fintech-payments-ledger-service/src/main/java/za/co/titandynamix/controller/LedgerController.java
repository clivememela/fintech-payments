package za.co.titandynamix.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.titandynamix.dto.CreateAccountRequest;
import za.co.titandynamix.dto.LedgerEntryRequest;
import za.co.titandynamix.dto.TransactionResult;
import za.co.titandynamix.dto.TransferStatusResponse;
import za.co.titandynamix.entity.Account;
import za.co.titandynamix.entity.LedgerEntry;
import za.co.titandynamix.repository.LedgerEntryRepository;
import za.co.titandynamix.service.LedgerService;
import za.co.titandynamix.service.RetryableLedgerService;
import za.co.titandynamix.service.TransferService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The type Ledger controller.
 * <p>
 *     This class provides REST endpoints for the following:
 *          - POST /accounts – create an account with an initial balance.
 *          - GET /accounts/{id} – fetch account with current balance and metadata.
 *          - POST /ledger/transfer – apply a transfer (debit from one account, credit to another).
 *              Body: {transferId, fromAccountId, toAccountId, amount}.
 *                  Single atomic call to Ledger Service POST /ledger/transfer that handles both debit and credit operations within a single database transaction.
 *          - GET /ledger/transfers/{id} – fetch status for a transfer by its transferId.
 * </p>
 * <p>
 *     Ledger Service ensures atomicity of the actual balance changes.
 * </p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Ledger Operations", description = "APIs for account management and atomic double-entry bookkeeping")
class LedgerController {
    private final LedgerService ledgerService;
    private final RetryableLedgerService retryableLedgerService;
    private final TransferService transferService;

    /**
     * POST /accounts – create an account with an initial balance.
     *
     * @param createAccountRequest the creation account request
     * @return response entity
     */
    @PostMapping("/accounts")
    public ResponseEntity<Account> createAccount(@RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody CreateAccountRequest createAccountRequest) {
        return ResponseEntity.ok(ledgerService.createAccount(idempotencyKey, createAccountRequest));
    }

    /**
     * Add transaction response entity.
     *
     * @param ledgerEntryRequest the ledger entry request
     * @return the response entity
     */
    @PostMapping("/add-transaction)")
    public ResponseEntity<String> addTransaction(@RequestBody LedgerEntryRequest ledgerEntryRequest){
        transferService.addTransaction(ledgerEntryRequest);
        return ResponseEntity.ok("Transaction created/added");
    }

    /**
     * Gets all accounts.
     *
     * @return all accounts
     */
    @GetMapping("/accounts")
    public ResponseEntity<List<Account>> getAllAccounts() {
        return ResponseEntity.ok(ledgerService.getAllAccounts());
    }

    /**
     * GET /accounts/{id} – fetch account with current balance and metadata.
     *
     * @param accountId the account id
     * @return response entity
     */
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<Account> getAccountByAccountId(@PathVariable Long accountId) {
        return ResponseEntity.ok(ledgerService.getAccountByAccountId(accountId));
    }

    /**
     * Get account balance by account ID response entity.
     *
     * @param accountId the account id
     * @return the response entity
     */
    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<BigDecimal> getAccountBalanceByAccountId(@PathVariable Long accountId){
        return ResponseEntity.ok(ledgerService.getAccountBalanceByAccountId(accountId));
    }

    /**
     * POST /ledger/transfer – apply a transfer (debit from one account, credit to another).
     * Body: {transferId, fromAccountId, toAccountId, amount}.
     * <p>
     * Single atomic call to Ledger Service POST /ledger/transfer that handles both debit and credit operations within a single database transaction.
     *
     * @param ledgerEntryRequest the ledger entry request
     * @return success /failure.
     */
    @PostMapping("/ledger/transfer")
    public ResponseEntity<?> applyLedgerTransfer(@RequestBody LedgerEntryRequest ledgerEntryRequest) {
        TransactionResult result = ledgerService.createDoubleEntryTransaction(ledgerEntryRequest);

        if (result.isSuccess())
            return ResponseEntity.ok(apiResponse("success", result.getMessage()));

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(apiResponse("failure", result.getMessage()));
    }

    /**
     * POST /ledger/transfers – create and process a transfer with idempotency support.
     * This endpoint is called by the Transfer Service to create and execute transfers.
     * 
     * @param request the transfer creation request
     * @param idempotencyKey optional idempotency key header
     * @return the created transfer with status
     */
    @PostMapping("/ledger/transfers")
    public ResponseEntity<TransferCreationResponse> createAndProcessTransfer(
            @RequestBody TransferCreationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        try {
            UUID transferId = UUID.randomUUID();
            
            LedgerEntryRequest ledgerEntryRequest = new LedgerEntryRequest();
            ledgerEntryRequest.setTransferId(transferId);
            ledgerEntryRequest.setFromAccountId(request.fromAccountId());
            ledgerEntryRequest.setToAccountId(request.toAccountId());
            ledgerEntryRequest.setAmount(request.amount());
            
            TransactionResult result = retryableLedgerService.createDoubleEntryTransactionWithRetry(ledgerEntryRequest);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(new TransferCreationResponse(
                        transferId, 
                        "SUCCEEDED", 
                        "Transfer completed successfully"
                ));
            } else {
                return ResponseEntity.ok(new TransferCreationResponse(
                        transferId, 
                        "FAILED", 
                        result.getMessage()
                ));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TransferCreationResponse(
                            UUID.randomUUID(), 
                            "ERROR", 
                            "System error: " + e.getMessage()
                    ));
        }
    }

    /**
     * GET /ledger/transfers/{id} – fetch status for a transfer by its transferId.
     */
    @GetMapping("/ledger/transfers/{id}")
    public ResponseEntity<TransferStatusResponse> getTransferStatus(@PathVariable UUID id) {
        boolean processed = !ledgerServiceEntriesByTransferId(id).isEmpty();
        String status = processed ? "SUCCEEDED" : "PENDING";
        String message = processed ? "Transfer applied" : "Not yet applied";
        return ResponseEntity.ok(new za.co.titandynamix.dto.TransferStatusResponse(id, status, message));
    }

    // Helper to access repository without exposing it in interface; kept package-private for brevity
    @Autowired
    private LedgerEntryRepository _ledgerEntryRepository;
    private List<LedgerEntry> ledgerServiceEntriesByTransferId(UUID id) {
        return _ledgerEntryRepository.findAllByTransferId(id);
    }

    private Map<String, Object> apiResponse(String status, String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        map.put("message", message);
        return map;
    }

    /**
     * Request DTO for transfer creation
     */
    public record TransferCreationRequest(
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount
    ) {}

    /**
     * Response DTO for transfer creation
     */
    public record TransferCreationResponse(
            UUID transferId,
            String status,
            String message
    ) {}
}
