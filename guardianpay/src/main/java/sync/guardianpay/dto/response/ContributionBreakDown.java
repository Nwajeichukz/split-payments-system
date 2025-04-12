package sync.guardianpay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ContributionBreakDown {
    private BigDecimal initiatingParentContribution;

    private BigDecimal secondParentContribution;

}
