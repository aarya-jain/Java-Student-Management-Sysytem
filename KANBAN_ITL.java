import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;

/*
 * Student Management System + Kanban Board (Java Swing)
 * Student features: Add, View, Search, Update, Delete, Total, Percentage, Grade
 * Student data: students.csv
 * Kanban features: Add, Edit, Delete tasks and move them between statuses
 * Task data: tasks.csv
 * Run: javac KANBAN_ITL.java && java KANBAN_ITL
 */

class Student {
    String roll;
    String name;
    String branch;
    int[] marks;

    Student(String roll, String name, String branch, int[] marks) {
        this.roll = roll;
        this.name = name;
        this.branch = branch;
        this.marks = marks;
    }

    int getTotal() {
        int total = 0;
        for (int mark : marks) total += mark;
        return total;
    }

    double getPercentage() {
        return getTotal() / (double) marks.length;
    }

    String getGrade() {
        double percentage = getPercentage();
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 50) return "D";
        return "F";
    }
}

class Task {
    int id;
    String title;
    String description;
    String priority;
    String status;

    Task(int id, String title, String description, String priority, String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
    }
}

public class KANBAN_ITL extends JFrame {
    private static final String STUDENT_FILE = "students.csv";
    private static final String TASK_FILE = "tasks.csv";
    private static final String[] STATUSES = {"To Do", "In Progress", "Done"};

    // Student management data and GUI
    private final ArrayList<Student> students = new ArrayList<>();
    private JTextField rollField, nameField, branchField, searchField;
    private JTextField[] markFields = new JTextField[5];
    private JTable studentTable;
    private DefaultTableModel studentTableModel;

    // Kanban data and GUI
    private final ArrayList<Task> tasks = new ArrayList<>();
    private int nextTaskId = 1;
    private JTextField taskTitleField;
    private JTextArea taskDescriptionArea;
    private JComboBox<String> priorityCombo, statusCombo;
    private JTable[] kanbanTables = new JTable[3];
    private DefaultTableModel[] kanbanModels = new DefaultTableModel[3];
    private JTextField taskIdField;

    public KANBAN_ITL() {
        setTitle("Student Management System + Kanban Board");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Student Management", createStudentPanel());
        tabs.addTab("Kanban Board", createKanbanPanel());
        add(tabs);

        loadStudentsFromFile();
        refreshStudentTable(students);
        loadTasksFromFile();
        refreshKanbanBoard();
    }

    // ======================== STUDENT MANAGEMENT ========================
    private JPanel createStudentPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel form = new JPanel(new GridLayout(4, 4, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Student Details"));
        rollField = new JTextField();
        nameField = new JTextField();
        branchField = new JTextField();
        form.add(new JLabel("Roll No:")); form.add(rollField);
        form.add(new JLabel("Name:")); form.add(nameField);
        form.add(new JLabel("Branch:")); form.add(branchField);
        for (int i = 0; i < 5; i++) {
            markFields[i] = new JTextField();
            form.add(new JLabel("Subject " + (i + 1) + " Marks:"));
            form.add(markFields[i]);
        }

        JButton addButton = new JButton("Add Student");
        JButton updateButton = new JButton("Update Selected");
        JButton deleteButton = new JButton("Delete Selected");
        JButton clearButton = new JButton("Clear Form");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.add(addButton); buttons.add(updateButton); buttons.add(deleteButton); buttons.add(clearButton);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);

        String[] columns = {"Roll No", "Name", "Branch", "Total", "Percentage", "Grade"};
        studentTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        studentTable = new JTable(studentTableModel);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        root.add(new JScrollPane(studentTable), BorderLayout.CENTER);

        searchField = new JTextField(18);
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        searchPanel.add(new JLabel("Search by roll number or name:"));
        searchPanel.add(searchField); searchPanel.add(searchButton); searchPanel.add(showAllButton);
        root.add(searchPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addStudent());
        updateButton.addActionListener(e -> updateStudent());
        deleteButton.addActionListener(e -> deleteStudent());
        clearButton.addActionListener(e -> clearStudentForm());
        searchButton.addActionListener(e -> searchStudents());
        showAllButton.addActionListener(e -> { searchField.setText(""); refreshStudentTable(students); });
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && studentTable.getSelectedRow() >= 0) {
                int row = studentTable.convertRowIndexToModel(studentTable.getSelectedRow());
                Student selected = findStudentByRoll(String.valueOf(studentTableModel.getValueAt(row, 0)));
                if (selected != null) fillStudentForm(selected);
            }
        });
        return root;
    }

    private Student readStudentForm() {
        String roll = rollField.getText().trim();
        String name = cleanCsvText(nameField.getText());
        String branch = cleanCsvText(branchField.getText());
        if (roll.isEmpty() || name.isEmpty() || branch.isEmpty()) {
            showError("Please fill in roll number, name and branch.");
            return null;
        }
        int[] marks = new int[5];
        for (int i = 0; i < 5; i++) {
            try { marks[i] = Integer.parseInt(markFields[i].getText().trim()); }
            catch (NumberFormatException ex) { showError("Enter numeric marks for all five subjects."); return null; }
            if (marks[i] < 0 || marks[i] > 100) { showError("Marks must be between 0 and 100."); return null; }
        }
        return new Student(roll, name, branch, marks);
    }

    private void addStudent() {
        Student student = readStudentForm();
        if (student == null) return;
        if (findStudentByRoll(student.roll) != null) { showError("This roll number already exists."); return; }
        students.add(student);
        saveStudentsToFile();
        refreshStudentTable(students);
        clearStudentForm();
        JOptionPane.showMessageDialog(this, "Student added successfully.");
    }

    private void updateStudent() {
        int row = studentTable.getSelectedRow();
        if (row < 0) { showError("Select a student from the table first."); return; }
        row = studentTable.convertRowIndexToModel(row);
        String oldRoll = String.valueOf(studentTableModel.getValueAt(row, 0));
        Student updated = readStudentForm();
        if (updated == null) return;
        Student duplicate = findStudentByRoll(updated.roll);
        if (duplicate != null && !updated.roll.equalsIgnoreCase(oldRoll)) {
            showError("Another student already has this roll number."); return;
        }
        Student old = findStudentByRoll(oldRoll);
        if (old != null) students.set(students.indexOf(old), updated);
        saveStudentsToFile();
        refreshStudentTable(students);
        clearStudentForm();
        JOptionPane.showMessageDialog(this, "Student updated successfully.");
    }

    private void deleteStudent() {
        int row = studentTable.getSelectedRow();
        if (row < 0) { showError("Select a student from the table first."); return; }
        row = studentTable.convertRowIndexToModel(row);
        String roll = String.valueOf(studentTableModel.getValueAt(row, 0));
        int answer = JOptionPane.showConfirmDialog(this, "Delete student with roll number " + roll + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            Student student = findStudentByRoll(roll);
            if (student != null) students.remove(student);
            saveStudentsToFile();
            refreshStudentTable(students);
            clearStudentForm();
        }
    }

    private void searchStudents() {
        String query = searchField.getText().trim().toLowerCase();
        ArrayList<Student> results = new ArrayList<>();
        for (Student student : students) {
            if (student.roll.toLowerCase().contains(query) || student.name.toLowerCase().contains(query)) results.add(student);
        }
        refreshStudentTable(results);
        if (results.isEmpty()) JOptionPane.showMessageDialog(this, "No student found.");
    }

    private Student findStudentByRoll(String roll) {
        for (Student student : students) if (student.roll.equalsIgnoreCase(roll.trim())) return student;
        return null;
    }

    private void refreshStudentTable(ArrayList<Student> list) {
        studentTableModel.setRowCount(0);
        for (Student s : list) {
            studentTableModel.addRow(new Object[]{s.roll, s.name, s.branch, s.getTotal(),
                    String.format("%.2f%%", s.getPercentage()), s.getGrade()});
        }
    }

    private void fillStudentForm(Student s) {
        rollField.setText(s.roll); nameField.setText(s.name); branchField.setText(s.branch);
        for (int i = 0; i < 5; i++) markFields[i].setText(String.valueOf(s.marks[i]));
    }

    private void clearStudentForm() {
        rollField.setText(""); nameField.setText(""); branchField.setText("");
        for (JTextField field : markFields) field.setText("");
        studentTable.clearSelection();
    }

    private void saveStudentsToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(STUDENT_FILE))) {
            for (Student s : students) {
                writer.println(s.roll + "," + cleanCsvText(s.name) + "," + cleanCsvText(s.branch) + "," +
                        s.marks[0] + "," + s.marks[1] + "," + s.marks[2] + "," + s.marks[3] + "," + s.marks[4]);
            }
        } catch (IOException ex) { showError("Could not save students.csv: " + ex.getMessage()); }
    }

    private void loadStudentsFromFile() {
        File file = new File(STUDENT_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length != 8) continue;
                try {
                    int[] marks = new int[5];
                    for (int i = 0; i < 5; i++) marks[i] = Integer.parseInt(parts[i + 3]);
                    if (findStudentByRoll(parts[0]) == null) students.add(new Student(parts[0], parts[1], parts[2], marks));
                } catch (NumberFormatException ignored) { /* Skip invalid row */ }
            }
        } catch (IOException ex) { showError("Could not load students.csv: " + ex.getMessage()); }
    }

    // ============================== KANBAN ==============================
    private JPanel createKanbanPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Task Details"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 5, 4, 5); c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 0;
        taskIdField = new JTextField(5); taskIdField.setEditable(false); taskIdField.setText("New");
        taskTitleField = new JTextField(18);
        taskDescriptionArea = new JTextArea(2, 18); taskDescriptionArea.setLineWrap(true); taskDescriptionArea.setWrapStyleWord(true);
        priorityCombo = new JComboBox<>(new String[]{"High", "Medium", "Low"});
        statusCombo = new JComboBox<>(STATUSES);
        addFormRow(form, c, 0, "Task ID:", taskIdField);
        addFormRow(form, c, 1, "Title:", taskTitleField);
        addFormRow(form, c, 2, "Description:", new JScrollPane(taskDescriptionArea));
        addFormRow(form, c, 3, "Priority:", priorityCombo);
        addFormRow(form, c, 4, "Status:", statusCombo);

        JButton addTaskButton = new JButton("Add Task");
        JButton updateTaskButton = new JButton("Update / Move Task");
        JButton deleteTaskButton = new JButton("Delete Task");
        JButton clearTaskButton = new JButton("Clear Form");
        JPanel taskButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        taskButtons.add(addTaskButton); taskButtons.add(updateTaskButton); taskButtons.add(deleteTaskButton); taskButtons.add(clearTaskButton);
        JPanel top = new JPanel(new BorderLayout(4, 4)); top.add(form, BorderLayout.CENTER); top.add(taskButtons, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);

        JPanel board = new JPanel(new GridLayout(1, 3, 8, 0));
        String[] taskColumns = {"ID", "Task", "Priority", "Description"};
        for (int i = 0; i < STATUSES.length; i++) {
            kanbanModels[i] = new DefaultTableModel(taskColumns, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            kanbanTables[i] = new JTable(kanbanModels[i]);
            kanbanTables[i].setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            kanbanTables[i].setRowHeight(24);
            JPanel column = new JPanel(new BorderLayout(4, 4));
            column.setBorder(BorderFactory.createTitledBorder(STATUSES[i]));
            column.add(new JScrollPane(kanbanTables[i]), BorderLayout.CENTER);
            board.add(column);
            final int columnIndex = i;
            kanbanTables[i].getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && kanbanTables[columnIndex].getSelectedRow() >= 0) {
                    int selectedRow = kanbanTables[columnIndex].getSelectedRow();
                    int id = Integer.parseInt(String.valueOf(kanbanModels[columnIndex].getValueAt(selectedRow, 0)));
                    Task task = findTaskById(id);
                    if (task != null) fillTaskForm(task);
                }
            });
        }
        root.add(board, BorderLayout.CENTER);

        addTaskButton.addActionListener(e -> addTask());
        updateTaskButton.addActionListener(e -> updateTask());
        deleteTaskButton.addActionListener(e -> deleteTask());
        clearTaskButton.addActionListener(e -> clearTaskForm());
        return root;
    }

    private void addFormRow(JPanel panel, GridBagConstraints c, int row, String label, Component input) {
        c.gridx = 0; c.gridy = row; c.weightx = 0; panel.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1; panel.add(input, c);
    }

    private void addTask() {
        String title = cleanCsvText(taskTitleField.getText());
        String description = cleanCsvText(taskDescriptionArea.getText());
        if (title.isEmpty()) { showError("Please enter a task title."); return; }
        Task task = new Task(nextTaskId++, title, description,
                String.valueOf(priorityCombo.getSelectedItem()), String.valueOf(statusCombo.getSelectedItem()));
        tasks.add(task);
        saveTasksToFile(); refreshKanbanBoard(); clearTaskForm();
        JOptionPane.showMessageDialog(this, "Task added successfully.");
    }

    private void updateTask() {
        int id;
        try { id = Integer.parseInt(taskIdField.getText()); }
        catch (NumberFormatException ex) { showError("Select an existing task from the Kanban board first."); return; }
        Task task = findTaskById(id);
        if (task == null) { showError("Task not found."); return; }
        String title = cleanCsvText(taskTitleField.getText());
        if (title.isEmpty()) { showError("Please enter a task title."); return; }
        task.title = title;
        task.description = cleanCsvText(taskDescriptionArea.getText());
        task.priority = String.valueOf(priorityCombo.getSelectedItem());
        task.status = String.valueOf(statusCombo.getSelectedItem());
        saveTasksToFile(); refreshKanbanBoard(); clearTaskForm();
        JOptionPane.showMessageDialog(this, "Task updated / moved successfully.");
    }

    private void deleteTask() {
        int id;
        try { id = Integer.parseInt(taskIdField.getText()); }
        catch (NumberFormatException ex) { showError("Select a task from the board first."); return; }
        Task task = findTaskById(id);
        if (task == null) { showError("Task not found."); return; }
        int answer = JOptionPane.showConfirmDialog(this, "Delete task: " + task.title + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            tasks.remove(task); saveTasksToFile(); refreshKanbanBoard(); clearTaskForm();
        }
    }

    private Task findTaskById(int id) {
        for (Task task : tasks) if (task.id == id) return task;
        return null;
    }

    private void fillTaskForm(Task task) {
        taskIdField.setText(String.valueOf(task.id));
        taskTitleField.setText(task.title);
        taskDescriptionArea.setText(task.description);
        priorityCombo.setSelectedItem(task.priority);
        statusCombo.setSelectedItem(task.status);
        // Clear selections in other columns so only one task is selected.
        for (JTable table : kanbanTables) if (table.getSelectedRow() >= 0) {
            int selectedId = Integer.parseInt(String.valueOf(table.getModel().getValueAt(table.getSelectedRow(), 0)));
            if (selectedId != task.id) table.clearSelection();
        }
    }

    private void clearTaskForm() {
        taskIdField.setText("New"); taskTitleField.setText(""); taskDescriptionArea.setText("");
        priorityCombo.setSelectedItem("Medium"); statusCombo.setSelectedItem("To Do");
        for (JTable table : kanbanTables) table.clearSelection();
    }

    private void refreshKanbanBoard() {
        for (DefaultTableModel model : kanbanModels) if (model != null) model.setRowCount(0);
        for (Task task : tasks) {
            int statusIndex = statusIndex(task.status);
            if (statusIndex >= 0 && kanbanModels[statusIndex] != null) {
                kanbanModels[statusIndex].addRow(new Object[]{task.id, task.title, task.priority, task.description});
            }
        }
    }

    private int statusIndex(String status) {
        for (int i = 0; i < STATUSES.length; i++) if (STATUSES[i].equals(status)) return i;
        return 0;
    }

    private void saveTasksToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(TASK_FILE))) {
            for (Task task : tasks) {
                writer.println(task.id + "," + cleanCsvText(task.title) + "," + cleanCsvText(task.description) + "," +
                        task.priority + "," + task.status);
            }
        } catch (IOException ex) { showError("Could not save tasks.csv: " + ex.getMessage()); }
    }

    private void loadTasksFromFile() {
        File file = new File(TASK_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length != 5) continue;
                try {
                    int id = Integer.parseInt(parts[0]);
                    String status = parts[4];
                    if (statusIndex(status) < 0 || !isValidStatus(status)) status = "To Do";
                    tasks.add(new Task(id, parts[1], parts[2], parts[3], status));
                    if (id >= nextTaskId) nextTaskId = id + 1;
                } catch (NumberFormatException ignored) { /* Skip invalid row */ }
            }
        } catch (IOException ex) { showError("Could not load tasks.csv: " + ex.getMessage()); }
    }

    private boolean isValidStatus(String status) {
        for (String value : STATUSES) if (value.equals(status)) return true;
        return false;
    }

    // This simple CSV format replaces commas/newlines to avoid breaking rows.
    private String cleanCsvText(String text) {
        if (text == null) return "";
        return text.trim().replace(',', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new KANBAN_ITL().setVisible(true));
    }
}
