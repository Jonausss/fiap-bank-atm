package com.fiap.bank.atm.infrastructure.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionFactory {
    private static final String URL = "jdbc:sqlite:fiap_bank_v2.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver do SQLite não encontrado na memória: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeSchema() {
        String createAccountTable = """
                    CREATE TABLE IF NOT EXISTS tb_account (
                        id VARCHAR(36) PRIMARY KEY,
                        agency VARCHAR(10) NOT NULL,
                        number VARCHAR(20) NOT NULL,
                        balance DECIMAL(15, 2) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        pin VARCHAR(4) NOT NULL DEFAULT '1234',
                        daily_limit DECIMAL(15, 2) NOT NULL DEFAULT 1500.00
                    );
                """;

        String createTransactionTable = """
                    CREATE TABLE IF NOT EXISTS tb_transaction (
                        id VARCHAR(36) PRIMARY KEY,
                        account_id VARCHAR(36) NOT NULL,
                        type VARCHAR(20) NOT NULL,
                        amount DECIMAL(15, 2) NOT NULL,
                        created_at TIMESTAMP NOT NULL,
                        description VARCHAR(255),
                        FOREIGN KEY (account_id) REFERENCES tb_account (id)
                    );
                """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createAccountTable);
            stmt.execute(createTransactionTable);
        } catch (SQLException e) {
            System.err.println("Erro Crítico ao criar schema: " + e.getMessage());
        }
    }
}