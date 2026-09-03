package com.example.demo.account.mail;

import com.example.demo.account.AccountRecoveryService;

/** Sends a server-only recovery envelope to the configured transactional mail workflow. */
public interface AccountMailGateway {
    boolean deliver(AccountRecoveryService.Delivery delivery);
    boolean notifyPasswordChanged(String recipient);
}
