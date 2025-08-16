package za.co.titandynamix.service;

import za.co.titandynamix.dto.TransferRequest;
import za.co.titandynamix.entity.Transfer;

import java.util.List;
import java.util.UUID;

public interface PaymentTransferService {
    Transfer getTransferStatus(TransferRequest request, String idempotencyKey);
    Transfer getTransferStatus(UUID id);
    List<Transfer> createTransfersBatch(List<TransferRequest> requests);
}