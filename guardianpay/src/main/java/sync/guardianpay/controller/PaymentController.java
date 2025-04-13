package sync.guardianpay.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import sync.guardianpay.dto.request.ProcessingDto;
import sync.guardianpay.dto.response.AppResponse;
import sync.guardianpay.model.Payment;
import sync.guardianpay.service.PaymentService;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("payment_processing")
    public AppResponse<Payment> processPayments(@Valid @RequestBody ProcessingDto processingDto){

        return paymentService.processPayments(processingDto);
    }


}
