package sync.guardianpay.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sync.guardianpay.dto.request.ProcessingDto;
import sync.guardianpay.dto.response.AppResponse;
import sync.guardianpay.dto.response.ContributionBreakDown;
import sync.guardianpay.enums.PaymentStatusEnum;
import sync.guardianpay.exception.ApiException;
import sync.guardianpay.model.Parent;
import sync.guardianpay.model.Payment;
import sync.guardianpay.model.Student;
import org.springframework.transaction.annotation.Transactional;
import sync.guardianpay.repository.ParentRepository;
import sync.guardianpay.repository.StudentRepository;
import sync.guardianpay.repository.TransactionRepository;
import sync.guardianpay.service.PaymentService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;


@Service
@RequiredArgsConstructor
public class PaymentServiceImp implements PaymentService {
    private final ParentRepository parentRepository;

    private final StudentRepository studentRepository;

    private final TransactionRepository transactionRepository;

    private final PaymentPersistenceService paymentPersistenceService;

    @Transactional
    @Override
    public AppResponse<Payment> processPayments(ProcessingDto processingDto) {
        //fetch student or throw error if not found
        Student student = studentRepository.findById(processingDto.getStudentId())
                .orElseThrow(() -> new ApiException("student not found"));

        BigDecimal realAmount =  processingDto.getPaymentAmount();

        //calculate dynamic rate
        BigDecimal dynamicRate = calculateDynamicRate(student, realAmount);

        //calculate adjusted amount
         BigDecimal adjustedAmount = realAmount.multiply(BigDecimal.ONE.add(dynamicRate));

        //create transaction record with failed status
        Payment transaction = createFailedTransaction(processingDto, dynamicRate,adjustedAmount);

        // Process payment based on the number of parents
        List<Parent> parents = student.getParents();

        if (parents.size() == 1){
            return processSingleParentPayment(parents.get(0), student, adjustedAmount, transaction, realAmount);
        }else if(parents.size() == 2){
            return processTwoParentPayment(parents, student, adjustedAmount, transaction, realAmount);
        }

        throw new ApiException("Invalid number of parents linked to student.");
    }

    private AppResponse<Payment> processSingleParentPayment(Parent uniqueParent, Student student,
                                                            BigDecimal adjustedAmount, Payment transaction,
                                                            BigDecimal realAmount){
        // verify parent making payments is linked to student
        if (!uniqueParent.getId().equals(transaction.getParentId())){
            throw new ApiException("No Relationship Between this Student and parent");
        }


        //deduct and credit balance
        processUniqueStudentPayment(uniqueParent, student, adjustedAmount, realAmount);

        //update the transaction to success
        transaction.setStatus(PaymentStatusEnum.SUCCESS);
        Payment savedTransaction = transactionRepository.save(transaction);

        return new AppResponse<>("Success", savedTransaction);
    }

    private AppResponse<Payment> processTwoParentPayment(List<Parent> parents, Student student,
                                                         BigDecimal adjustedAmount, Payment transaction, BigDecimal realAmount){
        Parent initiatingParent = null;
        Parent secondParent = null;

        // initiating and second parent
        for (Parent parent: parents){
            if (parent.getId().equals(transaction.getParentId())){
                initiatingParent = parent;
            }else{
                secondParent = parent;
            }
        }

        // validate relationship
        if (initiatingParent == null){
            throw new ApiException("No Relationship Between this Student and parent");
        }

        //process using both parents
        processSharedStudentPayment(initiatingParent, secondParent, student, adjustedAmount, realAmount);

        //update transaction status to success
        transaction.setStatus(PaymentStatusEnum.SUCCESS);
        Payment savedTransaction = transactionRepository.save(transaction);

        return new AppResponse<>("Success", savedTransaction);

    }


    private BigDecimal calculateDynamicRate(Student student, BigDecimal amount){
        BigDecimal dynamicRate = new BigDecimal("0.02"); //base rate

        if (amount.compareTo(new BigDecimal("1000")) > 0){
            dynamicRate = dynamicRate.add(new BigDecimal("0.02")); // add 2% if amount > 1000
        }

        if (isSharedStudent(student)) {
            dynamicRate = dynamicRate.add(new BigDecimal("0.005")); // add 0.5% if student is shared
        }

        return dynamicRate;
    }

    private boolean isSharedStudent(Student student){
        return student.getParents().size() > 1;
    }

    private void processUniqueStudentPayment(Parent parent, Student student, BigDecimal amount, BigDecimal realAmount){
        //Check if Funds Sufficient
        if (amount.compareTo(parent.getBalance()) > 0) throw new ApiException("Insufficient Funds");

        //update parents account
        parent.setBalance(parent.getBalance().subtract(amount));
        parentRepository.save(parent);

        //update students account
        student.setBalance(student.getBalance().add(realAmount));
        studentRepository.save(student);
    }

    private void processSharedStudentPayment(Parent initiatingParent, Parent secondParent,
                                             Student student, BigDecimal adjustedAmount,BigDecimal realAmount){
        BigDecimal initiatingParentBalance = initiatingParent.getBalance();
        BigDecimal secondParentBalance = secondParent.getBalance();


        //Check if Funds Sufficient
        if (adjustedAmount.compareTo(initiatingParentBalance.add(secondParentBalance)) > 0)
            throw new ApiException("Insufficient Funds");

        //calculate possible contribution percentage
        ContributionBreakDown contributions =
                calculateContribution(initiatingParentBalance, secondParentBalance,adjustedAmount);

        //update parents account
        initiatingParent.setBalance(initiatingParentBalance.subtract(contributions.getInitiatingParentContribution()));
        parentRepository.save(initiatingParent);

        secondParent.setBalance(secondParentBalance.subtract(contributions.getSecondParentContribution()));
        parentRepository.save(secondParent);

        //update students account
        student.setBalance(student.getBalance().add(realAmount));
        studentRepository.save(student);
    }

    private Payment createFailedTransaction(ProcessingDto processingDto, BigDecimal dynamicRate, BigDecimal adjustedAmount){
        Payment transaction = new Payment();
        transaction.setParentId(processingDto.getParentId());
        transaction.setStudentId(processingDto.getStudentId());
        transaction.setDynamicRate(dynamicRate);
        transaction.setOriginalAmount(processingDto.getPaymentAmount());
        transaction.setAdjustedAmount(adjustedAmount);
        transaction.setStatus(PaymentStatusEnum.FAILED);

        //Transaction failed persisted
        paymentPersistenceService.saveFailedTransaction(transaction);

        return transaction;
    }

    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal percentage){
        return amount.multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private ContributionBreakDown calculateContribution(BigDecimal initiatingParentBalance,
                                                        BigDecimal secondParentBalance,
                                                        BigDecimal adjustedAmount){
        // percent shares
        BigDecimal sixtyPercent = calculatePercentage(adjustedAmount, new BigDecimal("60"));
        BigDecimal fortyPercent = calculatePercentage(adjustedAmount, new BigDecimal("40"));
        BigDecimal twentyPercent = calculatePercentage(adjustedAmount, new BigDecimal("20"));
        BigDecimal eightyPercent = calculatePercentage(adjustedAmount, new BigDecimal("80"));

        //initiating parent checks
        boolean initParentSixtyCheck = initiatingParentBalance.compareTo(sixtyPercent) >=0;
        boolean initParentFortyCheck = initiatingParentBalance.compareTo(fortyPercent) >= 0;
        boolean initParentTwentyCheck = initiatingParentBalance.compareTo(twentyPercent) >= 0;

        //second parent checks
        boolean secondParentFortyCheck = secondParentBalance.compareTo(fortyPercent) >= 0;
        boolean secondParentSixtyCheck = secondParentBalance.compareTo(twentyPercent) >= 0;
        boolean secondParentEightyCheck = secondParentBalance.compareTo(eightyPercent) >= 0;

        if (!initParentTwentyCheck) throw new ApiException("initiating parents must contribute at least 20% of the amount");

        // Decide contributions
        if (initParentSixtyCheck && secondParentFortyCheck){
            return new ContributionBreakDown(sixtyPercent, fortyPercent);
        }else if(initParentFortyCheck && secondParentSixtyCheck){
            return new ContributionBreakDown(fortyPercent, sixtyPercent);
        } else if (secondParentEightyCheck) {
            return new ContributionBreakDown(twentyPercent, eightyPercent);
        }

        throw new ApiException("No Suitable contribution arrangements with current balance");
    }
}