package banking;

import java.sql.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import static java.lang.Math.pow;

public class Main {
    public static void main(String[] args) {

        String fileName = args[1];
        Connection conn = connectDatabase(args[1]);

        int choice;
        ArrayList<Account> accounts = new ArrayList<>();
        boolean repeat = true;
        while (repeat) {
            System.out.println("1. Create an account");
            System.out.println("2. Log into account");
            System.out.println("0. Exit");

            Scanner scanner = new Scanner(System.in);
            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    accounts.add(createAcc(accounts, conn));
                    break;
                case 2:
                    if (!logAcc(accounts, conn)) repeat = false;
                    else break;
                case 0:
                    repeat = false;
            }
        }
        System.out.println("Bye!");
        try {
            conn.close();
        } catch (SQLException e) {
            e.getMessage();
        }

    }

    public static Connection connectDatabase(String fileName) {
        String url = "jdbc:sqlite:./" + fileName;
        String sql = "CREATE TABLE IF NOT EXISTS card ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " number TEXT NOT NULL,"
                + " pin TEXT NOT NULL,"
                + " balance INTEGER DEFAULT 0"
                + ");";
        //     System.out.println(sql);
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;

    }

    public static void saveAcc(Connection conn, String number, String pin) {
        String sql = "INSERT INTO card(number, pin) VALUES(?,?)";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, number);
            stmt.setString(2, pin);

            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }


    public static Account createAcc(ArrayList<Account> accounts, Connection conn) {
        Account account;
        while (true) {
            account = new Account();
            boolean found = false;
            for (int i = 0; i < accounts.size() - 1; i++) {
                if (account.accNumber == accounts.get(i).accNumber) found = true;

            }
            if (!found) break;
        }
        System.out.println("Your card has been created");
        System.out.println("Your card number:");
        System.out.format("%16.0f%n", account.accNumber);
        System.out.println("Your card PIN:");
        System.out.format("%04d%n", account.pin);
        saveAcc(conn, account.number, String.format("%04d", account.pin));
        return account;
    }

    //вход в аккаунт
    public static boolean logAcc(ArrayList<Account> accounts, Connection conn) {
        System.out.println("Enter your card number");
        Scanner scanner = new Scanner(System.in);
        double accNumber = scanner.nextDouble();
        System.out.println("Enter your PIN");
        int pin = scanner.nextInt();
        boolean rightNumber = false;
        Account acc = new Account();
        for (int i = 0; i < accounts.size(); i++) {
            acc = accounts.get(i);
            if (acc.pin == pin && acc.accNumber == accNumber) {
                rightNumber = true;
                break;
            }
        }
        if (!rightNumber) {
            System.out.println("Wrong card number or PIN!");
            return true;
        }
        System.out.println("You have successfully logged in!");
        return accWork(acc, conn);
    }

    //процедура для работы со счетом
    public static boolean accWork(Account account, Connection conn) {
        int choice;
        while (true) {
            System.out.println("1. Balance");
            System.out.println("2. Add income");
            System.out.println("3. Do transfer");
            System.out.println("4. Close account");
            System.out.println("5. Log out");
            System.out.println("0. Exit");
            Scanner scanner = new Scanner(System.in);
            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    System.out.println("Balance: " + queryBalance(account.number, conn));
                    break;
                case 2:
                    System.out.println("Enter income: ");
                    int income = scanner.nextInt();
                    addIncome(account.number, income, conn);
                    System.out.println("Income was added!");
                    break;
                case 3:
                    System.out.println("Transfer");
                    System.out.println("Enter card number: ");
                    String targetAcc = scanner.next();
                    //+ проверить совпадают ли счета
                    if (account.number.equals(targetAcc)) {
                        System.out.println("You can't transfer money to the same account!");
                        break;
                    }

                    //+ проверить контрольный разряд
                    if (check(Double.parseDouble(targetAcc)) != targetAcc.charAt(15)) {
                        System.out.println("Probably you made mistake in the card number. Please try again!");
                        break;
                    }

                    //+ проверить, существует ли счет
                    if (! queryAcc(targetAcc, conn)) {
                        System.out.println("Such a card does not exist.");
                        break;
                    }

                    //+ запросить сумму
                    System.out.println("Enter how much money you want to transfer:");
                    int sum = scanner.nextInt();

                    //+ проверить достаточно ли денег на счете
                    int balance = queryBalance(account.number, conn);
                    if (sum > balance) {
                        System.out.println("Not enough money!");
                        break;
                    }

                    //+ выполнить трансфер
                    doTransfer(account.number, targetAcc, sum, conn);
                    System.out.println("Success!");
                    break;
                case 4:
                    //close account
                    closeAcc(account.number, conn);
                    System.out.println("The account has been closed!");
                    return true;
                case 5:
                    //log out
                    return true;

                case 0:
                    return false;
            }
        }
    }

    public static boolean queryAcc(String number, Connection conn) {
        String sql = "SELECT id FROM card WHERE number = ?";
        try {
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setString(1, number);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) return true;
            else return false;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }


    public static int queryBalance(String number, Connection conn) {
        String sql = "SELECT balance FROM card WHERE number = ?";
        try {
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setString(1, number);
            ResultSet rs = pstm.executeQuery();
            rs.next();
            return rs.getInt("balance");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return 0;
    }

    public static void addIncome(String number, int sum, Connection conn) {
        String sql = "UPDATE card SET balance = balance + ? WHERE number = ?";
        try {
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setInt(1, sum);
            pstm.setString(2, number);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            ;
        }
    }

    public static void closeAcc(String number, Connection conn) {
        String sql = "DELETE FROM card WHERE number = ?";
        try {
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setString(1, number);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void doTransfer(String number, String targetNumber, int sum, Connection conn) {
        addIncome(number, -sum, conn);
        addIncome(targetNumber, sum, conn);

    }

    //процедура вычисляет контрольную сумму
    public static int check(double number) {
        ArrayList<Integer> array = new ArrayList();
        int sum = 0;
        for (int i = 0; i < 15; i++) {
            double divisor = pow(10, 15 - i);
            int digit = (int) (number / divisor);
            if (i % 2 == 0) {
                digit = 2 * digit;
            }
            if (digit > 9) {
                digit = digit - 9;
            }
            sum += digit;
            number = number % divisor;
        }
        System.out.println(sum);
        int last = sum % 10;
        System.out.println(last);
        return 10 - last;
    }

    public static class Account {
        int pin;
        double accNumber;
        int balance;
        String number;

        public Account() {
            //сгенерировать номер счета и пин
            Random random = new Random();
            pin = random.nextInt(10000);
            double interval = 10000000000.;
            double rand = random.nextDouble();
            int last = (int) (rand * interval) % 10;//- rand * interval / 10;
            accNumber = (int) (rand * interval) - last + 4000000000000000.;
            accNumber = accNumber + check(accNumber);
            number = String.format("%16.0f", accNumber);
        }
    }
}

