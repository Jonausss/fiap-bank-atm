package com.fiap.bank.atm.presentation;

import com.fiap.bank.atm.application.service.AtmService;
import javax.swing.SwingUtilities;

public class AtmApplication {
    public static void main(String[] args) {
        try {
            Class<?> repoClass = Class.forName("com.fiap.bank.atm.infrastructure.persistence.InMemoryAccountRepository");
            Object repositoryInstance = repoClass.getDeclaredConstructor().newInstance();

            AtmService atmService = null;
            for (var constructor : AtmService.class.getConstructors()) {
                if (constructor.getParameterCount() == 1) {
                    atmService = (AtmService) constructor.newInstance(repositoryInstance);
                    break;
                }
            }

            final AtmService finalAtmService = atmService;

            SwingUtilities.invokeLater(() -> {
                AtmFrame mainFrame = new AtmFrame(finalAtmService);
                mainFrame.setVisible(true);
            });

        } catch (Exception e) {
            System.err.println("Erro crítico ao inicializar o motor da aplicação: " + e.getMessage());
            e.printStackTrace();
        }
    }
}