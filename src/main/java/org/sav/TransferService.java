package org.sav;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис по переводу денег
 */
public class TransferService {
    private static final Logger log = LogManager.getLogger(TransferService.class);

    private final List<Account> accounts;
    private final int maxTransactions;
    private final Random random;
    private final AtomicInteger transactionCount;
    private final Semaphore transactionSemaphore;

    public TransferService(List<Account> accounts, int maxTransactions) {
        this.accounts = accounts;
        this.maxTransactions = maxTransactions;
        this.random = new Random();
        transactionCount = new AtomicInteger(0);
        this.transactionSemaphore = new Semaphore(maxTransactions);
    }

    /**
     * Перевод между рандомными счетами
     *
     * @return успешность / не успешность
     */
    public boolean performRandomTransfer() {

        // Пытаемся атомарно зарезервировать слот до выполнения
        if (!transactionSemaphore.tryAcquire()) {
            return false; // Лимит достигнут
        }

        if (!canPerformTransfer()) {
            return false;
        }

        int[] randomIndex = random.ints(0, accounts.size())
                .distinct()
                .limit(2)
                .toArray();

        Account fromAccount = accounts.get(randomIndex[0]);
        Account toAccount = accounts.get(randomIndex[1]);

        Account firstLock;
        Account secondLock;

        // Определяем порядок блокировки счетов, чтобы не возникло ситуации deadlock
        if (fromAccount.getId().compareTo(toAccount.getId()) < 0) {
            firstLock = fromAccount;
            secondLock = toAccount;
        } else {
            firstLock = toAccount;
            secondLock = fromAccount;
        }

        boolean success = false;

        try {

            // Блокируем счета в определенном порядке для предотвращения deadlock
            synchronized (firstLock) {
                synchronized (secondLock) {
                    int amount = random.nextInt(1000) + 1;

                    // Проверяем, достаточно ли средств у отправителя
                    if (fromAccount.getMoney() >= amount) {
                        // Операция перевода
                        fromAccount.setMoney(fromAccount.getMoney() - amount);
                        toAccount.setMoney(toAccount.getMoney() + amount);

                        log.info("TRANSFER-{} SUCCESS: fromAccount: {} -> toAccount: {}, amount: {}, balance fromAccount: {}, balance toAccount: {}",
                                transactionCount.get(),
                                fromAccount.getId(),
                                toAccount.getId(),
                                amount,
                                fromAccount.getMoney(),
                                toAccount.getMoney());

                        success = true;
                    } else {
                        // Неудачная попытка перевода
                        log.warn("TRANSFER-{} FAILED: Insufficient funds. {} -> {}, requested amount: {}, available: {}",
                                transactionCount.get(),
                                fromAccount.getId(),
                                toAccount.getId(),
                                amount,
                                fromAccount.getMoney());
                    }
                }
            }

            if (success) {
                transactionCount.incrementAndGet();
            }

            return success;

        } finally {
            //  Возвращаем слот если перевод не удался
            if (!success) {
                transactionSemaphore.release();
            }
        }
    }

    /**
     * Проверка на количество счётов
     *
     * @return успешность
     */
    public boolean canPerformTransfer() {
        return accounts.size() >= 2;
    }

    public int getTransactionCount() {
        return transactionCount.get();
    }

    public int getMaxTransactions() {
        return maxTransactions;
    }

}
