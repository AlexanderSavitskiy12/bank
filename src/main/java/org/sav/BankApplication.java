package org.sav;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Банковское приложение по переводу денег между счетами
 */
public class BankApplication {
    private static final Logger log = LogManager.getLogger(BankApplication.class);

    private static final int INITIAL_BALANCE = 10000;   // Начальный баланс счёта
    private static final int NUMBER_ACCOUNTS = 4;       // Количество счетов в банке
    private static final int NUMBER_THREADS = 3;       // Количество потоков для обработки
    private static final int MAX_TRANSACTIONS = 30;     // Максимальное количество транзакции

    /**
     * Создание фиксированного количества счетов
     * @return - Список счетов
     */
    private static List<Account> createAccounts(){
        List<Account> accounts = new ArrayList<>();

        for (int i = 0; i < NUMBER_ACCOUNTS; i++) {
            String accountId = "account-" + i + "-" + UUID.randomUUID();
            accounts.add(new Account(accountId, INITIAL_BALANCE));
        }

        log.info("Created {} accounts with initial balance: {}", accounts.size(), INITIAL_BALANCE);

        return accounts;
    }

    public static void main(String[] args) {
        log.info("Bank application starting");

        try {
            List<Account> accounts = createAccounts();

            // Общая сумма счетов до выполнения операции переводов
            int initialTotalMoney = accounts.stream().mapToInt(Account::getMoney).sum();
            log.info("Initial total money: {}", initialTotalMoney);

            // Cервис для переводов
            TransferService transferService = new TransferService(accounts, MAX_TRANSACTIONS);

            ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_THREADS);

            for (int i = 0; i < NUMBER_THREADS; i++) {
                executorService.execute(new TransferRunnable(transferService));
            }

            executorService.shutdown();

            if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }

            int finalTotalMoney = accounts.stream().mapToInt(Account::getMoney).sum();
            log.info("Final total money: {}", finalTotalMoney);
            log.info("Total transactions performed: {}", transferService.getTransactionCount());

            log.debug("List of accounts:");
            accounts.forEach(log::debug);

            // Проверяем сохранение общей суммы
            if (initialTotalMoney == finalTotalMoney) {
                log.info("SUCCESS: Total money correctly");
            } else {
                log.error("The total amounts of money do not match! Initial: {}, Final: {}", initialTotalMoney, finalTotalMoney);
            }

        } catch (Exception e) {
            log.error("Critical error in application: {}", e.getMessage(), e);
        }

        log.info("Bank application finished");
    }

}