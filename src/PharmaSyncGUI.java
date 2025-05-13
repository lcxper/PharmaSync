import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;

class Medicine {
    private int id;
    private String name;
    private int quantity;
    private String expiryDate;

    public Medicine(int id, String name, int quantity, String expiryDate) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getExpiryDate() {
        return expiryDate;
    }
}

public class PharmaSyncGUI {
    private DefaultTableModel tableModel;
    private Connection connection;

    public PharmaSyncGUI() {
        // Setup JFrame
        JFrame frame = new JFrame("PharmaSync: Pharmacy Inventory System");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Database Connection
        try {
            connectToDatabase();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Database Connection Failed: " + e.getMessage());
            return;
        }

        // Title
        JLabel titleLabel = new JLabel("PharmaSync: Streamlined Pharmacy Inventory System", JLabel.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 20));
        frame.add(titleLabel, BorderLayout.NORTH);

        // Table for inventory
        String[] columns = {"ID", "Medicine Name", "Quantity", "Expiry Date"};
        tableModel = new DefaultTableModel(columns, 0);
        JTable inventoryTable = new JTable(tableModel);
        inventoryTable.setEnabled(false);
        JScrollPane tableScrollPane = new JScrollPane(inventoryTable);
        frame.add(tableScrollPane, BorderLayout.CENTER);

        // Buttons Panel (South)
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Medicine");
        JButton removeButton = new JButton("Remove Medicine");
        JButton checkExpiryButton = new JButton("Check Expiration");
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(checkExpiryButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        // Load Medicines into Table
        loadMedicines();

        // Adding a Medicine
        addButton.addActionListener(e -> openAddMedicineDialog(frame));

        // Removing a Medicine
        removeButton.addActionListener(e -> openRemoveMedicineDialog(frame));

        // Check Expiry
        checkExpiryButton.addActionListener(e -> openCheckExpiryDialog(frame));

        // Show the frame
        frame.setVisible(true);
    }

    private void connectToDatabase() throws SQLException, ClassNotFoundException {
        String url = "jdbc:mysql://localhost:3306/PharmaSyncDB";
        String username = "root";
        String password = "password"; // Replace this with your MySQL password

        Class.forName("com.mysql.cj.jdbc.Driver");
        connection = DriverManager.getConnection(url, username, password);
    }

    private void loadMedicines() {
        try {
            tableModel.setRowCount(0); // Clear existing data
            String query = "SELECT * FROM inventory";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                int quantity = resultSet.getInt("quantity");
                String expiryDate = resultSet.getString("expiry_date");

                tableModel.addRow(new Object[]{id, name, quantity, expiryDate});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void openAddMedicineDialog(JFrame parentFrame) {
        // Add Medicine Dialog
        JDialog addDialog = new JDialog(parentFrame, "Add Medicine", true);
        addDialog.setSize(300, 250);
        addDialog.setLayout(new GridLayout(4, 2));

        // Labels and Fields
        JLabel nameLabel = new JLabel("Medicine Name:");
        JTextField nameField = new JTextField();
        JLabel quantityLabel = new JLabel("Quantity:");
        JTextField quantityField = new JTextField();
        JLabel expiryLabel = new JLabel("Expiry Date (YYYY-MM-DD):");
        JTextField expiryField = new JTextField();

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> {
            String name = nameField.getText();
            int quantity;
            String expiryDate = expiryField.getText();

            try {
                quantity = Integer.parseInt(quantityField.getText());
                if (name.isEmpty() || expiryDate.isEmpty()) {
                    throw new IllegalArgumentException("Fields cannot be empty!");
                }

                addMedicine(name, quantity, expiryDate);
                addDialog.dispose();
                JOptionPane.showMessageDialog(parentFrame, "Medicine added successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(addDialog, "Invalid Input: " + ex.getMessage());
            }
        });

        addDialog.add(nameLabel);
        addDialog.add(nameField);
        addDialog.add(quantityLabel);
        addDialog.add(quantityField);
        addDialog.add(expiryLabel);
        addDialog.add(expiryField);
        addDialog.add(new JLabel()); // Empty cell
        addDialog.add(addButton);

        addDialog.setVisible(true);
    }

    private void openRemoveMedicineDialog(JFrame parentFrame) {
        String medicineName = JOptionPane.showInputDialog(parentFrame, "Enter the medicine name to remove:");
        if (medicineName != null) {
            boolean removed = removeMedicine(medicineName);
            if (removed) {
                JOptionPane.showMessageDialog(parentFrame, "Medicine removed successfully.");
            } else {
                JOptionPane.showMessageDialog(parentFrame, "Medicine not found.");
            }
        }
    }

    private void openCheckExpiryDialog(JFrame parentFrame) {
        String givenDate = JOptionPane.showInputDialog(parentFrame, "Enter the date to check expiry (YYYY-MM-DD):");
        if (givenDate != null) {
            ArrayList<Medicine> expiredMeds = checkExpiry(givenDate);
            if (expiredMeds.isEmpty()) {
                JOptionPane.showMessageDialog(parentFrame, "No medicines near or past expiry.");
            } else {
                StringBuilder message = new StringBuilder("Medicines nearing/past expiry:\n");
                for (Medicine med : expiredMeds) {
                    message.append(med.getName()).append(" (Expiry: ").append(med.getExpiryDate()).append(")\n");
                }
                JOptionPane.showMessageDialog(parentFrame, message.toString());
            }
        }
    }

    private void addMedicine(String name, int quantity, String expiryDate) {
        try {
            String query = "INSERT INTO inventory (name, quantity, expiry_date) VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, name);
            statement.setInt(2, quantity);
            statement.setString(3, expiryDate);
            statement.executeUpdate();

            loadMedicines();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean removeMedicine(String name) {
        try {
            String query = "DELETE FROM inventory WHERE name = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, name);
            int rows = statement.executeUpdate();

            if (rows > 0) {
                loadMedicines();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private ArrayList<Medicine> checkExpiry(String date) {
        ArrayList<Medicine> expiredMeds = new ArrayList<>();
        try {
            String query = "SELECT * FROM inventory WHERE expiry_date <= ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, date);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                int quantity = resultSet.getInt("quantity");
                String expiryDate = resultSet.getString("expiry_date");

                expiredMeds.add(new Medicine(id, name, quantity, expiryDate));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return expiredMeds;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PharmaSyncGUI::new);
    }
}