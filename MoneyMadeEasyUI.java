import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MoneyMadeEasyUI {
    private static final String[] CATEGORIES = {"Food", "Transport", "Bills", "Savings", "Other"};
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JFrame frame = new JFrame("Money Made Easy");
    private final JLabel profileLabel = new JLabel("No profile selected");
    private final JLabel balanceLabel = new JLabel("Balance: 0.00");
    private final JLabel monthLabel = new JLabel("This month: 0.00 income / 0.00 expense");
    private final DefaultTableModel transactionModel = new DefaultTableModel(
            new Object[]{"Date", "Type", "Amount", "Category", "Description"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    private String profile = "Student";
    private Ledger ledger;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MoneyMadeEasyUI().show());
    }

    public void show() {
        ledger = new Ledger(Paths.get("finance_" + profile.toLowerCase() + "_.txt"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(980, 680);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(35, 83, 71));
        header.setBorder(new EmptyBorder(18, 22, 18, 22));

        JLabel title = new JLabel("Money Made Easy");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        title.setForeground(Color.WHITE);

        profileLabel.setForeground(new Color(220, 238, 232));
        profileLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        header.add(title, BorderLayout.WEST);
        header.add(profileLabel, BorderLayout.EAST);

        JPanel stats = new JPanel(new GridLayout(1, 2, 12, 0));
        stats.setOpaque(false);
        stats.setBorder(new EmptyBorder(14, 22, 14, 22));

        JPanel main = new JPanel(new BorderLayout(12, 12));
        main.setBackground(new Color(244, 247, 245));
        main.setBorder(new EmptyBorder(0, 0, 12, 0));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(stats, BorderLayout.CENTER);

        balanceLabel.setOpaque(true);
        balanceLabel.setBackground(Color.WHITE);
        balanceLabel.setBorder(new EmptyBorder(14, 16, 14, 16));
        balanceLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));

        monthLabel.setOpaque(true);
        monthLabel.setBackground(Color.WHITE);
        monthLabel.setBorder(new EmptyBorder(14, 16, 14, 16));

        stats.add(balanceLabel);
        stats.add(monthLabel);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(0, 22, 12, 22));

        JTable table = new JTable(transactionModel);
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tablePane = new JScrollPane(table);
        tablePane.setBorder(BorderFactory.createLineBorder(new Color(220, 226, 222)));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        actions.setOpaque(false);
        JButton income = actionButton("Add income", new Color(52, 118, 91));
        JButton expense = actionButton("Add expense", new Color(174, 82, 65));
        JButton budget = actionButton("Set budget", new Color(67, 105, 137));
        JButton summary = actionButton("Summary", new Color(92, 92, 92));
        JButton delete = actionButton("Delete selected", new Color(92, 92, 92));
        JButton switchProfile = actionButton("Switch profile", new Color(92, 92, 92));

        income.addActionListener(e -> openTransactionDialog(TransactionType.INCOME));
        expense.addActionListener(e -> openTransactionDialog(TransactionType.EXPENSE));
        budget.addActionListener(e -> openBudgetDialog());
        summary.addActionListener(e -> showSummary());
        delete.addActionListener(e -> deleteSelected(table));
        switchProfile.addActionListener(e -> switchProfile(table));

        actions.add(income);
        actions.add(expense);
        actions.add(budget);
        actions.add(summary);
        actions.add(delete);
        actions.add(switchProfile);

        content.add(actions, BorderLayout.NORTH);
        content.add(tablePane, BorderLayout.CENTER);

        main.add(top, BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);

        frame.add(header, BorderLayout.NORTH);
        frame.add(main, BorderLayout.CENTER);

        switchProfile(profile, table);
        frame.setVisible(true);
    }

    private JButton actionButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void switchProfile(JTable table) {
        Object[] options = {"Student", "Worker"};
        int choice = JOptionPane.showOptionDialog(frame, "Choose a profile", "Profile",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice >= 0) {
            profile = (String) options[choice];
            switchProfile(profile, table);
        }
    }

    private void switchProfile(String selectedProfile, JTable table) {
        profile = selectedProfile;
        ledger = new Ledger(Paths.get("finance_" + profile.toLowerCase() + ".txt"));
        profileLabel.setText(profile + " mode");
        refresh(table);
    }

    private void openTransactionDialog(TransactionType type) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        JTextField amount = new JTextField();
        JComboBox<String> category = new JComboBox<>(CATEGORIES);
        JTextField description = new JTextField();

        panel.add(new JLabel("Amount:"));
        panel.add(amount);
        panel.add(new JLabel("Category:"));
        panel.add(category);
        panel.add(new JLabel("Description:"));
        panel.add(description);

        int result = JOptionPane.showConfirmDialog(frame, panel,
                type == TransactionType.INCOME ? "Add income" : "Add expense",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            String cleanAmount = amount.getText().trim().replace(",", "");
            BigDecimal value = new BigDecimal(cleanAmount);
            if (value.signum() <= 0) throw new NumberFormatException();

            String warning = "";
            if (type == TransactionType.EXPENSE) {
                warning = ledger.checkBudget((String) category.getSelectedItem(), value);
            }
            ledger.add(new Transaction(type == TransactionType.INCOME ? TransactionKind.INCOME : TransactionKind.EXPENSE,
                    value, (String) category.getSelectedItem(), description.getText(), LocalDateTime.now()));
            refresh(findTable());

            if (!warning.isEmpty()) {
                JOptionPane.showMessageDialog(frame, warning, "Budget notice", JOptionPane.WARNING_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Enter a valid positive amount.", "Invalid amount", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openBudgetDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        JComboBox<String> category = new JComboBox<>(CATEGORIES);
        JTextField limit = new JTextField();
        panel.add(new JLabel("Category:"));
        panel.add(category);
        panel.add(new JLabel("Monthly limit (0 removes):"));
        panel.add(limit);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Set budget",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            BigDecimal value = new BigDecimal(limit.getText().trim().replace(",", ""));
            if (value.signum() < 0) throw new NumberFormatException();
            String categoryName = (String) category.getSelectedItem();
            if (value.signum() == 0) ledger.removeBudget(categoryName);
            else ledger.setBudget(categoryName, value);
            refresh(findTable());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Enter a valid budget amount.", "Invalid amount", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelected(JTable table) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(frame, "Select a transaction first.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        int ledgerIndex = ledger.size() - 1 - modelRow;
        int confirm = JOptionPane.showConfirmDialog(frame, "Delete the selected transaction?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            ledger.remove(ledgerIndex);
            refresh(table);
        }
    }

    private void showSummary() {
        JTextArea area = new JTextArea(ledger.getSummary());
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JOptionPane.showMessageDialog(frame, new JScrollPane(area), "Spending summary", JOptionPane.INFORMATION_MESSAGE);
    }

    private JTable findTable() {
        for (Component c : frame.getContentPane().getComponents()) {
            if (c instanceof JPanel) {
                JTable found = findTable(c);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JTable findTable(Component c) {
        if (c instanceof JTable) return (JTable) c;
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                JTable found = findTable(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void refresh(JTable table) {
        transactionModel.setRowCount(0);
        List<Transaction> records = ledger.getAll();
        for (int i = records.size() - 1; i >= 0; i--) {
            Transaction t = records.get(i);
            transactionModel.addRow(new Object[]{
                    t.date.format(DISPLAY_DATE), t.kind.label, money(t.amount), t.category, t.description
            });
        }

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        YearMonth now = YearMonth.now();
        BigDecimal monthIncome = BigDecimal.ZERO;
        BigDecimal monthExpense = BigDecimal.ZERO;
        for (Transaction t : records) {
            if (t.kind == TransactionKind.INCOME) income = income.add(t.amount);
            else expense = expense.add(t.amount);
            if (YearMonth.from(t.date).equals(now)) {
                if (t.kind == TransactionKind.INCOME) monthIncome = monthIncome.add(t.amount);
                else monthExpense = monthExpense.add(t.amount);
            }
        }
        balanceLabel.setText("Balance: " + money(income.subtract(expense)));
        monthLabel.setText("This month: " + money(monthIncome) + " income / " + money(monthExpense) + " expense");
    }

    private static String money(BigDecimal value) {
        return String.format("%,.2f", value);
    }

    enum TransactionType { INCOME, EXPENSE }

    enum TransactionKind {
        INCOME("Income"), EXPENSE("Expense");
        final String label;
        TransactionKind(String label) { this.label = label; }
    }

    static class Transaction {
        final TransactionKind kind;
        final BigDecimal amount;
        final String category;
        final String description;
        final LocalDateTime date;

        Transaction(TransactionKind kind, BigDecimal amount, String category, String description, LocalDateTime date) {
            this.kind = kind;
            this.amount = amount;
            this.category = category;
            this.description = description;
            this.date = date;
        }

        String toLine() {
            return "T\t" + kind.name() + "\t" + amount.toPlainString() + "\t" + category
                    + "\t" + description.replaceAll("[\\t\\r\\n]+", " ").trim() + "\t" + date;
        }

        static Transaction fromLine(String[] parts) {
            return new Transaction(TransactionKind.valueOf(parts[1]), new BigDecimal(parts[2]), parts[3], parts[4],
                    LocalDateTime.parse(parts[5]));
        }
    }

    static class Ledger {
        private final Path file;
        private final List<Transaction> records = new ArrayList<>();
        private final Map<String, BigDecimal> budgets = new TreeMap<>();

        Ledger(Path file) {
            this.file = file;
            load();
        }

        int size() { return records.size(); }
        List<Transaction> getAll() { return records; }
        void add(Transaction transaction) { records.add(transaction); save(); }
        void remove(int index) { records.remove(index); save(); }
        void setBudget(String category, BigDecimal limit) { budgets.put(category, limit); save(); }
        void removeBudget(String category) { budgets.remove(category); save(); }

        BigDecimal spentInMonth(String category, YearMonth month) {
            BigDecimal total = BigDecimal.ZERO;
            for (Transaction transaction : records) {
                if (transaction.kind == TransactionKind.EXPENSE
                        && transaction.category.equalsIgnoreCase(category)
                        && YearMonth.from(transaction.date).equals(month)) {
                    total = total.add(transaction.amount);
                }
            }
            return total;
        }

        String checkBudget(String category, BigDecimal newAmount) {
            BigDecimal limit = budgets.get(category);
            if (limit == null) return "";
            BigDecimal spent = spentInMonth(category, YearMonth.now());
            BigDecimal after = spent.add(newAmount);
            if (after.compareTo(limit) > 0) {
                return "You are OVER your " + category + " budget. Limit: " + money(limit)
                        + "; after this entry: " + money(after);
            }
            if (after.compareTo(limit.multiply(new BigDecimal("0.80"))) >= 0) {
                return "You are approaching your " + category + " budget. After this entry: " + money(after);
            }
            return "";
        }

        String getSummary() {
            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;
            Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
            for (Transaction transaction : records) {
                if (transaction.kind == TransactionKind.INCOME) income = income.add(transaction.amount);
                else {
                    expense = expense.add(transaction.amount);
                    categoryTotals.merge(transaction.category, transaction.amount, BigDecimal::add);
                }
            }
            StringBuilder result = new StringBuilder();
            result.append("ALL-TIME SUMMARY\n\n");
            result.append(String.format("Income:  %,.2f%n", income));
            result.append(String.format("Expense: %,.2f%n", expense));
            result.append(String.format("Balance: %,.2f%n%n", income.subtract(expense)));
            result.append("Spending by category\n");
            if (categoryTotals.isEmpty()) result.append("No expenses yet\n");
            else for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
                result.append(String.format("%-14s %,.2f%n", entry.getKey(), entry.getValue()));
            }
            return result.toString();
        }

        private void save() {
            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> entry : budgets.entrySet()) {
                lines.add("B\t" + entry.getKey() + "\t" + entry.getValue().toPlainString());
            }
            for (Transaction transaction : records) lines.add(transaction.toLine());
            try { Files.write(file, lines, StandardCharsets.UTF_8); }
            catch (IOException exception) {
                JOptionPane.showMessageDialog(null, "Could not save data: " + exception.getMessage());
            }
        }

        private void load() {
            if (!Files.exists(file)) return;
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String[] parts = line.split("\t", -1);
                    if (parts.length == 6 && parts[0].equals("T")) records.add(Transaction.fromLine(parts));
                    else if (parts.length == 3 && parts[0].equals("B")) budgets.put(parts[1], new BigDecimal(parts[2]));
                }
            } catch (IOException | RuntimeException exception) {
                JOptionPane.showMessageDialog(null, "Could not read saved data. Starting fresh.");
            }
        }
    }
}
