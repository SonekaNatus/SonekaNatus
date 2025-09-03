import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;

public class LoginGui extends JFrame {
    private JTextField userField;
    private JPasswordField passField;
    private JButton btnLogin, btnCadastrar;

    private HashMap<String, String> usuarios = new HashMap<>();
    private final File arquivoUsuarios = new File("usuarios.txt");

    public LoginGui() {
        setTitle("Login");
        setSize(350, 150);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(3, 3, 5, 5));

        add(new JLabel("Usuário:"));
        userField = new JTextField();
        add(userField);

        add(new JLabel("Senha:"));
        passField = new JPasswordField();
        add(passField);
        btnLogin = new JButton("Entrar");
        add(btnLogin);
        btnCadastrar = new JButton("Cadastrar");
        add(btnCadastrar);

        loadUsuarios();

        btnLogin.addActionListener(e -> login());
        btnCadastrar.addActionListener(e -> cadastrar());

    }

    private void loadUsuarios() {
        if (!arquivoUsuarios.exists()) {
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(arquivoUsuarios))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] partes = linha.split(";");
                if (partes.length == 2) {
                    usuarios.put(partes[0], partes[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void salvarUsuarios() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(arquivoUsuarios))) {
            for (String usuario : usuarios.keySet()) {
                pw.println(usuario + ";" + usuarios.get(usuario));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void login() {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha usuário e senha.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (usuarios.containsKey(user) && usuarios.get(user).equals(pass)) {
            // Login ok
            new MainGUI().setVisible(true);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Usuário ou senha incorretos!", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cadastrar() {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha usuário e senha para cadastrar.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (usuarios.containsKey(user)) {
            JOptionPane.showMessageDialog(this, "Usuário já existe!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        usuarios.put(user, pass);
        salvarUsuarios();
        JOptionPane.showMessageDialog(this, "Usuário cadastrado com sucesso!");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginGui().setVisible(true));
    }
}
