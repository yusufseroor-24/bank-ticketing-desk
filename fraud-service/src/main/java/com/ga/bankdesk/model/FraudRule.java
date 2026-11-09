package com.ga.bankdesk.model;

import com.ga.bankdesk.enums.FraudRuleType;
import com.ga.bankdesk.enums.TicketPriority;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@Entity
@Table(name = "fraud_rules")
public class FraudRule {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //unique code to identity the rule
    @Column(nullable = false, unique = true)
    private String code;

    //the title shown in the auto created ticket
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FraudRuleType ruleType;

    @Column(precision = 19, scale = 2)
    private BigDecimal minAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal maxAmount;

    @Column(nullable = false)
    private String alertCategory;

    //how many
    private Integer threshold;

    //within how long
    private Integer windowMinutes;

    @Column(length = 500)
    private String countries;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketPriority alertPriority;

    @Column(nullable = false)
    private boolean enabled = true;
}
