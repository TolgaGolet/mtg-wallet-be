package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.repository.CategoryRepository;
import com.mtg.mtgwalletbe.repository.PayeeRepository;
import com.mtg.mtgwalletbe.util.Names;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mtg.mtgwalletbe.enums.TransactionType.*;
import static com.mtg.mtgwalletbe.service.DomainLimits.MAX_CATEGORIES;
import static com.mtg.mtgwalletbe.service.DomainLimits.MAX_PAYEES;

/**
 * Creates the starter categories and payees for a new user in two batched inserts (instead of ~55 service calls that
 * each re-checked limits and names). Defaults whose name already exists for the user are skipped, and an existing
 * category with a default parent's name is reused as that parent.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DefaultDataService {
    private final CategoryRepository categoryRepository;
    private final PayeeRepository payeeRepository;
    private final UserService userService;

    record DefaultCategory(String name, TransactionType type, String parent) {
    }

    record DefaultPayee(String name, TransactionType type, String category) {
    }

    // Parents must come before their children
    static final List<DefaultCategory> CATEGORIES = List.of(
            c("Auto", EXPENSE), c("Gas", EXPENSE, "Auto"), c("Service", EXPENSE, "Auto"),
            c("Bill", EXPENSE), c("Electricity", EXPENSE, "Bill"), c("Internet", EXPENSE, "Bill"), c("Phone", EXPENSE, "Bill"), c("Water", EXPENSE, "Bill"),
            c("Charity", EXPENSE), c("Clothing", EXPENSE), c("Education", EXPENSE),
            c("Eating Out", EXPENSE), c("Fast Food", EXPENSE, "Eating Out"),
            c("Electronics", EXPENSE),
            c("Entertainment", EXPENSE), c("Concert", EXPENSE, "Entertainment"), c("Digital Services", EXPENSE, "Entertainment"),
            c("Gifts", EXPENSE), c("Groceries", EXPENSE), c("Household", EXPENSE),
            c("Medical", EXPENSE), c("Hospital", EXPENSE, "Medical"), c("Pharmacy", EXPENSE, "Medical"),
            c("Others", EXPENSE),
            c("Personal Care", EXPENSE), c("Hairdresser", EXPENSE, "Personal Care"),
            c("Rent", EXPENSE), c("Tax", EXPENSE),
            c("Transport", EXPENSE), c("Bus", EXPENSE, "Transport"), c("Transportation Card", EXPENSE, "Transport"),
            c("Travel", EXPENSE),
            c("Bonus", INCOME), c("Interest", INCOME),
            c("Investment", INCOME), c("Cryptocurrency", INCOME, "Investment"), c("Gold", INCOME, "Investment"), c("Stocks", INCOME, "Investment"),
            c("Others", INCOME), c("Pocket Money", INCOME), c("Salary", INCOME),
            c("Credit Card Debt", TRANSFER), c("Investment", TRANSFER)
    );

    static final List<DefaultPayee> PAYEES = List.of(
            new DefaultPayee("Shell", EXPENSE, "Gas"),
            new DefaultPayee("Türk Telekom", EXPENSE, "Phone"),
            new DefaultPayee("Defacto", EXPENSE, "Clothing"),
            new DefaultPayee("Burger King", EXPENSE, "Fast Food"),
            new DefaultPayee("Spotify", EXPENSE, "Digital Services"),
            new DefaultPayee("Amazon Prime", EXPENSE, "Digital Services"),
            new DefaultPayee("Migros", EXPENSE, "Groceries"),
            new DefaultPayee("Istanbul Card", EXPENSE, "Transportation Card"),
            new DefaultPayee("Binance", INCOME, "Cryptocurrency"),
            new DefaultPayee("Midas", INCOME, "Stocks")
    );

    private static DefaultCategory c(String name, TransactionType type) {
        return new DefaultCategory(name, type, null);
    }

    private static DefaultCategory c(String name, TransactionType type, String parent) {
        return new DefaultCategory(name, type, parent);
    }

    public void createDefaultsForCurrentUser() {
        WalletUser user = userService.getCurrentUserEntity();
        if (Boolean.TRUE.equals(user.getIsDefaultsCreated())) {
            return;
        }
        // One query each for what the user already has
        List<Category> categories = new ArrayList<>(categoryRepository.findAllByUserIdAndStatus(user.getId(), Status.ACTIVE));
        List<String> existingPayeeNames = payeeRepository.findNamesByUserIdAndStatus(user.getId(), Status.ACTIVE, -1L);

        // Same per-user caps as CategoryService/PayeeService.create: defaults stop at the limit
        int categorySlots = MAX_CATEGORIES - categories.size();
        List<Category> newCategories = new ArrayList<>();
        for (DefaultCategory def : CATEGORIES) {
            if (find(categories, def.name(), def.type()).isPresent()) {
                continue;
            }
            if (newCategories.size() >= categorySlots) {
                break;
            }
            Category parent = def.parent() == null ? null : find(categories, def.parent(), def.type()).orElse(null);
            if (parent != null && parent.getParentCategory() != null) {
                parent = null; // never nest under a category that is itself a child
            }
            Category category = new Category();
            category.setName(def.name());
            category.setTransactionType(def.type());
            category.setUser(user);
            category.setParentCategory(parent);
            category.setIsParent(Boolean.FALSE);
            category.setStatus(Status.ACTIVE);
            if (parent != null) {
                parent.setIsParent(Boolean.TRUE);
            }
            categories.add(category);
            newCategories.add(category);
        }
        categoryRepository.saveAll(newCategories);

        int payeeSlots = MAX_PAYEES - existingPayeeNames.size();
        List<Payee> newPayees = new ArrayList<>();
        for (DefaultPayee def : PAYEES) {
            if (Names.containsName(existingPayeeNames, def.name())) {
                continue;
            }
            if (newPayees.size() >= payeeSlots) {
                break;
            }
            find(categories, def.category(), def.type()).ifPresent(category -> {
                Payee payee = new Payee();
                payee.setName(def.name());
                payee.setCategory(category);
                payee.setUser(user);
                payee.setStatus(Status.ACTIVE);
                newPayees.add(payee);
            });
        }
        payeeRepository.saveAll(newPayees);
        user.setIsDefaultsCreated(Boolean.TRUE);
    }

    private static Optional<Category> find(List<Category> categories, String name, TransactionType type) {
        return categories.stream()
                .filter(category -> category.getTransactionType() == type && Names.sameName(category.getName(), name))
                .findFirst();
    }
}
