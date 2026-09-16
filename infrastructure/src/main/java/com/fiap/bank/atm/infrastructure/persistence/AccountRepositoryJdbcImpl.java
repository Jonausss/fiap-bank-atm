package com.fiap.bank.atm.infrastructure.persistence;

import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.model.Transaction;
import com.fiap.bank.atm.domain.model.TransactionType;
import com.fiap.bank.atm.domain.repository.AccountRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AccountRepositoryJdbcImpl implements AccountRepository {

    public AccountRepositoryJdbcImpl() {
        seedInitialData();
    }

    private void seedInitialData() {
        String seedSql = """
                    INSERT OR IGNORE INTO tb_account (id, agency, number, balance, status, pin, daily_limit)
                    VALUES 
                    ('550e8400-e29b-41d4-a716-446655440000', '0001', '123456', 1500.00, 'ACTIVE', '1234', 1000.00),
                    ('550e8400-e29b-41d4-a716-446655440001', '0001', '987654', 250.50, 'ACTIVE', '5678', 500.00),
                    ('550e8400-e29b-41d4-a716-446655440002', '0002', '111111', 0.00, 'BLOCKED', '9999', 100.00);
                """;
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(seedSql);
        } catch (SQLException e) {
            System.err.println("Erro ao realizar seed de dados: " + e.getMessage());
        }
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        String sql = "SELECT * FROM tb_account WHERE number = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, accountNumber);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Account account = mapAccount(rs);
                    loadTransactions(account, conn);
                    return Optional.of(account);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Account> findById(UUID id) {
        String sql = "SELECT * FROM tb_account WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Account account = mapAccount(rs);
                    loadTransactions(account, conn);
                    return Optional.of(account);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public void save(Account account) {
        String checkSql = "SELECT 1 FROM tb_account WHERE id = ?";
        boolean exists = false;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement psCheck = conn.prepareStatement(checkSql)) {

            psCheck.setString(1, account.getId().toString());
            try (ResultSet rs = psCheck.executeQuery()) {
                exists = rs.next();
            }

            if (exists) {
                String updateSql = "UPDATE tb_account SET balance = ? WHERE id = ?";
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setBigDecimal(1, account.getBalance().getAmount());
                    psUpdate.setString(2, account.getId().toString());
                    psUpdate.executeUpdate();
                }
            } else {
                String insertSql = "INSERT INTO tb_account (id, agency, number, balance, status, pin, daily_limit) VALUES (?, '0001', ?, ?, 'ACTIVE', '1234', ?)";
                try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                    psInsert.setString(1, account.getId().toString());
                    psInsert.setString(2, account.getAccountNumber());
                    psInsert.setBigDecimal(3, account.getBalance().getAmount());
                    psInsert.setBigDecimal(4, account.getDailyWithdrawalLimit().getAmount());
                    psInsert.executeUpdate();
                }
            }

            saveTransactions(account, conn);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void remove(UUID id) {
        String sql = "DELETE FROM tb_account WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Account> findAll() {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM tb_account";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Account account = mapAccount(rs);
                loadTransactions(account, conn);
                accounts.add(account);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accounts;
    }

    private Account mapAccount(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        String number = rs.getString("number");
        String pin = rs.getString("pin");
        BigDecimal balance = rs.getBigDecimal("balance");
        BigDecimal dailyLimit = rs.getBigDecimal("daily_limit");

        return new Account(id, number, pin, Money.of(balance), Money.of(dailyLimit));
    }

    private void loadTransactions(Account account, Connection conn) throws SQLException {
        String sql = "SELECT * FROM tb_transaction WHERE account_id = ? ORDER BY created_at ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    TransactionType type = TransactionType.valueOf(rs.getString("type"));
                    BigDecimal amount = rs.getBigDecimal("amount");
                    LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
                    String description = rs.getString("description");

                    Transaction tx = new Transaction(id, createdAt, type, Money.of(amount), description);
                    account.seedTransaction(tx);
                }
            }
        }
    }

    private void saveTransactions(Account account, Connection conn) throws SQLException {
        String sql = "INSERT OR IGNORE INTO tb_transaction (id, account_id, type, amount, created_at, description) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Transaction tx : account.getTransactions()) {
                ps.setString(1, tx.getId().toString());
                ps.setString(2, account.getId().toString());
                ps.setString(3, tx.getType().name());
                ps.setBigDecimal(4, tx.getAmount().getAmount());
                ps.setTimestamp(5, Timestamp.valueOf(tx.getTimestamp()));
                ps.setString(6, tx.getDescription());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}