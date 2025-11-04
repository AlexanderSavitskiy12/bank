package org.sav;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

public class TransferRunnable implements Runnable {
    private static final Logger log = LogManager.getLogger(TransferRunnable.class);

    private final TransferService transferService;
    private final Random random;

    public TransferRunnable(TransferService transferService) {
        this.transferService = transferService;
        this.random = new Random();
    }

    @Override
    public void run() {
        log.info("Thread [{}] started", Thread.currentThread().getName());

        while (!Thread.currentThread().isInterrupted()) {

            // Проверяем общий лимит транзакций
            if (transferService.getTransactionCount() >= transferService.getMaxTransactions()) {
                log.debug("Max transactions reached. Thread [{}] stopping", Thread.currentThread().getName());
                break;
            }

            // Проверка счетов
            if (!transferService.canPerformTransfer()) {
                log.error("Not enough accounts for transfer");
                break;
            }

            try {
                // Случайная задержка от 1000 до 2000 мс
                Thread.sleep(random.nextInt(1000) + 1000);

                // Выполняем перевод
                if (transferService.performRandomTransfer()) {
                    log.debug("Completed transfer successfully");
                }else{
                    log.debug("Transfer not completed");
                }
            } catch (InterruptedException e) {
                log.warn("Thread was interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in thread: {}", e.getMessage(), e);
                break;
            }
        }

        log.info("Thread [{}] finished", Thread.currentThread().getName());
    }

}
