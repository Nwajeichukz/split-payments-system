package sync.guardianpay.service;

import sync.guardianpay.dto.request.ProcessingDto;
import sync.guardianpay.dto.response.AppResponse;
import sync.guardianpay.model.Payment;

import javax.validation.Valid;

public interface PaymentService {
    AppResponse<Payment> processPayments(@Valid ProcessingDto processingDto);
}
