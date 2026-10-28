package com.ga.bankdesk.config;

import com.ga.bankdesk.enums.FraudRuleType;
import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.model.FraudRule;
import com.ga.bankdesk.repository.FraudRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Component
@Order(4)
public class FraudRuleSeeder implements CommandLineRunner {

    private final FraudRuleRepository ruleRepository;

    private static final String FATF_BLACK_LIST = "IR,KP,MM";

    private static final String FATF_GREY_LIST = "AO,BO,BA,BG,CM,CI,CD,HT,IQ,KE,LA,LB,MC,NP,SS,SY,VE,VN,VG,YE,KW,PG";

    @Override
    public void run(String...args){
        if (ruleRepository.count() > 0) {
            return;
        }
        ruleRepository.save(rule("LARGE_TRANSACTION", "Large transaction", FraudRuleType.LARGE_AMOUNT,
                "10000.00", null, null, null, null, "FRAUD", TicketPriority.HIGH));
        ruleRepository.save(rule("STRUCTURING_DEPOSITS", "Possible structuring of cash deposits", FraudRuleType.STRUCTURING,
                "8000.00", "10000.00", 3, 1440, null, "AML", TicketPriority.HIGH));
        ruleRepository.save(rule("RAPID_ACTIVITY", "Rapid account activity", FraudRuleType.VELOCITY,
                null, null, 5, 10, null, "FRAUD", TicketPriority.MEDIUM));
        ruleRepository.save(rule("HIGH_RISK_COUNTRY", "Transfer to high-risk country", FraudRuleType.RISKY_COUNTRY,
                null, null, null, null, FATF_BLACK_LIST, "AML", TicketPriority.HIGH));
        ruleRepository.save(rule("GREY_LIST_COUNTRY", "Transfer to country under FATF monitoring", FraudRuleType.RISKY_COUNTRY,
                null, null, null, null, FATF_GREY_LIST, "AML", TicketPriority.MEDIUM));
    }

    private FraudRule rule(String code, String name, FraudRuleType type, String minAmount, String maxAmount, Integer threshold,
                           Integer windowMinutes, String countries, String alertCategory, TicketPriority priority) {
        FraudRule r = new FraudRule();
        r.setCode(code);
        r.setName(name);
        r.setRuleType(type);
        r.setMinAmount(minAmount == null ? null : new BigDecimal(minAmount));
        r.setMaxAmount(maxAmount == null ? null : new BigDecimal(maxAmount));
        r.setThreshold(threshold);
        r.setWindowMinutes(windowMinutes);
        r.setCountries(countries);
        r.setAlertCategory(alertCategory);
        r.setAlertPriority(priority);
        return r;
    }
}

