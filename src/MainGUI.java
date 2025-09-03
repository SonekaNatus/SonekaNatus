import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.text.ParseException;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.jdatepicker.impl.*;

public class MainGUI extends JFrame {

    private Estoque estoque;
    private int proximaId = 1;

    // Componentes GUI
    private JTextField nomeField, quantidadeField, precoField;
    private JTextField nomeAtualizarField, quantidadeAtualizarField, precoAtualizarField;
    private JTextField nomeRemoverField;

    private JTextField filtroProdutosField;
    private JTextField filtroHistoricoField;

    private JDatePickerImpl datePickerAdd;
    private JDatePickerImpl datePickerUpdate;

    private JTable tableProdutos;
    private DefaultTableModel tableModelProdutos;

    private JTable tableHistorico;
    private DefaultTableModel tableModelHistorico;

    private static final String ARQUIVO_ESTOQUE = "estoque.json";

    public MainGUI() {
        estoque = new Estoque();

        carregarEstoque();

        setTitle("Sistema de Estoque");
        setSize(1000, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Color fundoPrincipal = Color.decode("#F4F0FA");
        Color fundoSecundario = Color.decode("#E0D7F5");
        Color corPrincipal = Color.decode("#008000");
        Color corDestaque = Color.decode("#008000");

        getContentPane().setBackground(fundoPrincipal);

        UIManager.put("Button.background", corPrincipal);
        UIManager.put("Button.foreground", Color.white);

        JTabbedPane tabbedPane = new JTabbedPane();

        // --- ABA ADICIONAR ---

        JPanel addPanel = new JPanel(new GridBagLayout());
        addPanel.setBackground(fundoPrincipal);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        nomeField = new JTextField(20);
        quantidadeField = new JTextField(20);
        precoField = new JTextField(20);

        // DatePicker Adicionar
        UtilDateModel modelAdd = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Hoje");
        p.put("text.month", "Mês");
        p.put("text.year", "Ano");
        JDatePanelImpl datePanelAdd = new JDatePanelImpl(modelAdd, p);
        datePickerAdd = new JDatePickerImpl(datePanelAdd, new DateLabelFormatter());

        JButton btnAdd = new JButton("Adicionar Produto");

        // Layout adicionar
        gbc.gridx = 0; gbc.gridy = 0;
        addPanel.add(new JLabel("Nome:"), gbc);
        gbc.gridx = 1;
        addPanel.add(nomeField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        addPanel.add(new JLabel("Quantidade:"), gbc);
        gbc.gridx = 1;
        addPanel.add(quantidadeField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        addPanel.add(new JLabel("Preço (R$):"), gbc);
        gbc.gridx = 1;
        addPanel.add(precoField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        addPanel.add(new JLabel("Validade:"), gbc);
        gbc.gridx = 1;
        addPanel.add(datePickerAdd, gbc);

        gbc.gridx = 1; gbc.gridy = 4;
        addPanel.add(btnAdd, gbc);

        btnAdd.addActionListener(e -> adicionarProduto());

        tabbedPane.addTab("Adicionar", addPanel);

        // --- ABA LISTAR ---

        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBackground(fundoPrincipal);

        JPanel topoFiltroProdutos = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topoFiltroProdutos.setBackground(fundoPrincipal);
        filtroProdutosField = new JTextField(20);
        JButton btnFiltrarProdutos = new JButton("Buscar");
        JButton btnLimparFiltroProdutos = new JButton("Limpar");
        JButton btnExportarProdutos = new JButton("Exportar CSV");

        topoFiltroProdutos.add(new JLabel("Buscar produto: "));
        topoFiltroProdutos.add(filtroProdutosField);
        topoFiltroProdutos.add(btnFiltrarProdutos);
        topoFiltroProdutos.add(btnLimparFiltroProdutos);
        topoFiltroProdutos.add(btnExportarProdutos);

        listPanel.add(topoFiltroProdutos, BorderLayout.NORTH);

        String[] colunas = {"ID", "Nome", "Quantidade", "Preço (R$)", "Validade"};
        tableModelProdutos = new DefaultTableModel(colunas, 0);
        tableProdutos = new JTable(tableModelProdutos) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : fundoSecundario);
                }
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };

        tableProdutos.setRowHeight(30);
        tableProdutos.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tableProdutos.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        tableProdutos.getTableHeader().setBackground(corPrincipal);
        tableProdutos.getTableHeader().setForeground(Color.white);

        JScrollPane scrollProdutos = new JScrollPane(tableProdutos);
        listPanel.add(scrollProdutos, BorderLayout.CENTER);

        JButton btnAtualizarLista = new JButton("Atualizar Lista");
        btnAtualizarLista.setBackground(corDestaque);
        btnAtualizarLista.setForeground(Color.white);
        btnAtualizarLista.setFocusPainted(false);
        btnAtualizarLista.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel bottomProdutos = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomProdutos.setBackground(fundoPrincipal);
        bottomProdutos.add(btnAtualizarLista);
        listPanel.add(bottomProdutos, BorderLayout.SOUTH);

        tabbedPane.addTab("Listar", listPanel);

        // Ações filtro produtos
        btnFiltrarProdutos.addActionListener(e -> filtrarProdutos());
        btnLimparFiltroProdutos.addActionListener(e -> {
            filtroProdutosField.setText("");
            listarProdutos();
        });
        btnAtualizarLista.addActionListener(e -> listarProdutos());
        btnExportarProdutos.addActionListener(e -> exportarProdutosCSV());

        // --- ABA ATUALIZAR ---

        JPanel updatePanel = new JPanel(new GridBagLayout());
        updatePanel.setBackground(fundoPrincipal);

        nomeAtualizarField = new JTextField(20);
        quantidadeAtualizarField = new JTextField(20);
        precoAtualizarField = new JTextField(20);

        // DatePicker Atualizar
        UtilDateModel modelUpdate = new UtilDateModel();
        JDatePanelImpl datePanelUpdate = new JDatePanelImpl(modelUpdate, p);
        datePickerUpdate = new JDatePickerImpl(datePanelUpdate, new DateLabelFormatter());

        JButton btnAtualizar = new JButton("Atualizar Produto");

        gbc.gridx = 0; gbc.gridy = 0;
        updatePanel.add(new JLabel("Nome do produto:"), gbc);
        gbc.gridx = 1;
        updatePanel.add(nomeAtualizarField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        updatePanel.add(new JLabel("Nova quantidade:"), gbc);
        gbc.gridx = 1;
        updatePanel.add(quantidadeAtualizarField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        updatePanel.add(new JLabel("Novo preço (R$):"), gbc);
        gbc.gridx = 1;
        updatePanel.add(precoAtualizarField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        updatePanel.add(new JLabel("Nova validade:"), gbc);
        gbc.gridx = 1;
        updatePanel.add(datePickerUpdate, gbc);

        gbc.gridx = 1; gbc.gridy = 4;
        updatePanel.add(btnAtualizar, gbc);

        btnAtualizar.addActionListener(e -> atualizarProduto());

        tabbedPane.addTab("Atualizar", updatePanel);

        // --- ABA REMOVER ---

        JPanel removePanel = new JPanel(new GridBagLayout());
        removePanel.setBackground(fundoPrincipal);
        nomeRemoverField = new JTextField(20);
        JButton btnRemover = new JButton("Remover Produto");
        btnRemover.setBackground(Color.RED);
        btnRemover.setForeground(Color.WHITE);

        gbc.gridx = 0; gbc.gridy = 0;
        removePanel.add(new JLabel("Nome do produto:"), gbc);
        gbc.gridx = 1;
        removePanel.add(nomeRemoverField, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        removePanel.add(btnRemover, gbc);

        btnRemover.addActionListener(e -> removerProduto());

        tabbedPane.addTab("Remover", removePanel);

        // --- ABA HISTÓRICO ---

        JPanel historicoPanel = new JPanel(new BorderLayout());
        historicoPanel.setBackground(fundoPrincipal);

        JPanel topoFiltroHistorico = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topoFiltroHistorico.setBackground(fundoPrincipal);
        filtroHistoricoField = new JTextField(20);
        JButton btnFiltrarHistorico = new JButton("Buscar");
        JButton btnLimparFiltroHistorico = new JButton("Limpar");
        JButton btnExportarHistorico = new JButton("Exportar CSV");

        topoFiltroHistorico.add(new JLabel("Buscar histórico: "));
        topoFiltroHistorico.add(filtroHistoricoField);
        topoFiltroHistorico.add(btnFiltrarHistorico);
        topoFiltroHistorico.add(btnLimparFiltroHistorico);
        topoFiltroHistorico.add(btnExportarHistorico);

        historicoPanel.add(topoFiltroHistorico, BorderLayout.NORTH);

        String[] colunasHistorico = {"Nome Produto", "Quantidade", "Tipo", "Data e Hora"};
        tableModelHistorico = new DefaultTableModel(colunasHistorico, 0);
        tableHistorico = new JTable(tableModelHistorico) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : fundoSecundario);
                }
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };

        tableHistorico.setRowHeight(30);
        tableHistorico.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tableHistorico.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        tableHistorico.getTableHeader().setBackground(corPrincipal);
        tableHistorico.getTableHeader().setForeground(Color.white);

        JScrollPane scrollHistorico = new JScrollPane(tableHistorico);
        historicoPanel.add(scrollHistorico, BorderLayout.CENTER);

        JButton btnAtualizarHistorico = new JButton("Atualizar Histórico");
        btnAtualizarHistorico.setBackground(corDestaque);
        btnAtualizarHistorico.setForeground(Color.white);
        btnAtualizarHistorico.setFocusPainted(false);
        btnAtualizarHistorico.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel bottomHistorico = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomHistorico.setBackground(fundoPrincipal);
        bottomHistorico.add(btnAtualizarHistorico);
        historicoPanel.add(bottomHistorico, BorderLayout.SOUTH);

        tabbedPane.addTab("Histórico", historicoPanel);

        btnFiltrarHistorico.addActionListener(e -> filtrarHistorico());
        btnLimparFiltroHistorico.addActionListener(e -> {
            filtroHistoricoField.setText("");
            listarHistorico();
        });
        btnAtualizarHistorico.addActionListener(e -> listarHistorico());
        btnExportarHistorico.addActionListener(e -> exportarHistoricoCSV());

        // --- ABA BACKUP ---

        JPanel backupPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        backupPanel.setBackground(fundoPrincipal);
        JButton btnSalvarBackup = new JButton("Salvar Backup (JSON)");
        JButton btnRestaurarBackup = new JButton("Restaurar Backup (JSON)");

        backupPanel.add(btnSalvarBackup);
        backupPanel.add(btnRestaurarBackup);

        btnSalvarBackup.addActionListener(e -> salvarBackup());
        btnRestaurarBackup.addActionListener(e -> restaurarBackup());

        tabbedPane.addTab("Backup", backupPanel);

        add(tabbedPane);

        listarProdutos();
        listarHistorico();
    }

    // --- MÉTODOS ---

    private void adicionarProduto() {
        String nome = nomeField.getText().trim();
        String qtdStr = quantidadeField.getText().trim();
        String precoStr = precoField.getText().trim();
        Date selectedDate = (Date) datePickerAdd.getModel().getValue();

        if (nome.isEmpty() || qtdStr.isEmpty() || precoStr.isEmpty() || selectedDate == null) {
            JOptionPane.showMessageDialog(this, "Preencha todos os campos, incluindo a validade!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int qtd = Integer.parseInt(qtdStr);
            if (qtd <= 0) throw new NumberFormatException("Quantidade deve ser > 0");
            double preco = Double.parseDouble(precoStr);
            if (preco < 0) throw new NumberFormatException("Preço não pode ser negativo");
            LocalDate validade = selectedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            if (validade.isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(this, "Data de validade não pode ser no passado!", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Verifica se produto já existe (por nome, case insensitive)
            Produto produtoExistente = estoque.getProdutos().stream()
                    .filter(p -> p.getNome().equalsIgnoreCase(nome))
                    .findFirst()
                    .orElse(null);

            if (produtoExistente != null) {
                JOptionPane.showMessageDialog(this, "Produto já existe. Use Atualizar para modificar.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Produto p = new Produto(proximaId++, nome, qtd, preco, validade);
            estoque.adicionarProduto(p);
            estoque.adicionarEvento(new Evento(nome, qtd, "ADICIONADO", LocalDateTime.now()));

            salvarEstoque();

            JOptionPane.showMessageDialog(this, "Produto adicionado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            limparCamposAdicionar();
            listarProdutos();
            listarHistorico();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantidade e preço devem ser números válidos e positivos.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limparCamposAdicionar() {
        nomeField.setText("");
        quantidadeField.setText("");
        precoField.setText("");
        datePickerAdd.getModel().setValue(null);
    }

    private void listarProdutos() {
        tableModelProdutos.setRowCount(0);

        LocalDate hoje = LocalDate.now();

        for (Produto p : estoque.getProdutos()) {
            String validadeStr = p.getValidade().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            Object[] linha = {
                    p.getId(),
                    p.getNome(),
                    p.getQuantidade(),
                    String.format("%.2f", p.getPreco()),
                    validadeStr
            };

            tableModelProdutos.addRow(linha);
        }
        // Alerta sobre validade próxima
        verificarValidadeProdutos();
    }

    private void verificarValidadeProdutos() {
        List<Produto> proximosAVencer = estoque.getProdutos().stream()
                .filter(p -> {
                    LocalDate validade = p.getValidade();
                    LocalDate hoje = LocalDate.now();
                    return !validade.isBefore(hoje) && !validade.isAfter(hoje.plusDays(5));
                })
                .collect(Collectors.toList());

        if (!proximosAVencer.isEmpty()) {
            StringBuilder sb = new StringBuilder("Produtos com validade próxima (até 5 dias):\n");
            for (Produto p : proximosAVencer) {
                sb.append("- ").append(p.getNome()).append(" (Validade: ").append(p.getValidade().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append(")\n");
            }
            JOptionPane.showMessageDialog(this, sb.toString(), "Alerta de Validade", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void filtrarProdutos() {
        String filtro = filtroProdutosField.getText().trim().toLowerCase();
        tableModelProdutos.setRowCount(0);

        for (Produto p : estoque.getProdutos()) {
            if (p.getNome().toLowerCase().contains(filtro)) {
                String validadeStr = p.getValidade().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                Object[] linha = {
                        p.getId(),
                        p.getNome(),
                        p.getQuantidade(),
                        String.format("%.2f", p.getPreco()),
                        validadeStr
                };

                tableModelProdutos.addRow(linha);
            }
        }
    }

    private void atualizarProduto() {
        String nome = nomeAtualizarField.getText().trim();
        String qtdStr = quantidadeAtualizarField.getText().trim();
        String precoStr = precoAtualizarField.getText().trim();
        Date selectedDate = (Date) datePickerUpdate.getModel().getValue();

        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe o nome do produto a atualizar!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Produto p = estoque.getProdutos().stream()
                .filter(prod -> prod.getNome().equalsIgnoreCase(nome))
                .findFirst()
                .orElse(null);

        if (p == null) {
            JOptionPane.showMessageDialog(this, "Produto não encontrado!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (!qtdStr.isEmpty()) {
                int qtd = Integer.parseInt(qtdStr);
                if (qtd <= 0) throw new NumberFormatException();
                p.setQuantidade(qtd);
            }

            if (!precoStr.isEmpty()) {
                double preco = Double.parseDouble(precoStr);
                if (preco < 0) throw new NumberFormatException();
                p.setPreco(preco);
            }

            if (selectedDate != null) {
                LocalDate validade = selectedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                if (validade.isBefore(LocalDate.now())) {
                    JOptionPane.showMessageDialog(this, "Data de validade não pode ser no passado!", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                p.setValidade(validade);
            }

            estoque.adicionarEvento(new Evento(nome, p.getQuantidade(), "ATUALIZADO", LocalDateTime.now()));

            salvarEstoque();

            JOptionPane.showMessageDialog(this, "Produto atualizado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            listarProdutos();
            listarHistorico();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantidade e preço devem ser números válidos.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removerProduto() {
        String nome = nomeRemoverField.getText().trim();

        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe o nome do produto a remover!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Produto p = estoque.getProdutos().stream()
                .filter(prod -> prod.getNome().equalsIgnoreCase(nome))
                .findFirst()
                .orElse(null);

        if (p == null) {
            JOptionPane.showMessageDialog(this, "Produto não encontrado!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Tem certeza que deseja remover o produto \"" + nome + "\"?", "Confirmação", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            estoque.getProdutos().remove(p);
            estoque.adicionarEvento(new Evento(nome, p.getQuantidade(), "REMOVIDO", LocalDateTime.now()));

            salvarEstoque();

            JOptionPane.showMessageDialog(this, "Produto removido!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            listarProdutos();
            listarHistorico();
            nomeRemoverField.setText("");
        }
    }

    private void listarHistorico() {
        tableModelHistorico.setRowCount(0);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        for (Evento e : estoque.getHistorico()) {
            Object[] linha = {
                    e.getNomeProduto(),
                    e.getQuantidade(),
                    e.getTipo(),
                    e.getDataHora().format(formatter)
            };
            tableModelHistorico.addRow(linha);
        }
    }

    private void filtrarHistorico() {
        String filtro = filtroHistoricoField.getText().trim().toLowerCase();
        tableModelHistorico.setRowCount(0);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        for (Evento e : estoque.getHistorico()) {
            if (e.getNomeProduto().toLowerCase().contains(filtro) || e.getTipo().toLowerCase().contains(filtro)) {
                Object[] linha = {
                        e.getNomeProduto(),
                        e.getQuantidade(),
                        e.getTipo(),
                        e.getDataHora().format(formatter)
                };
                tableModelHistorico.addRow(linha);
            }
        }
    }

    private void exportarProdutosCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Salvar produtos como CSV");
        chooser.setSelectedFile(new File("produtos.csv"));
        int resp = chooser.showSaveDialog(this);
        if (resp == JFileChooser.APPROVE_OPTION) {
            File arquivo = chooser.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(arquivo)) {
                pw.println("ID,Nome,Quantidade,Preço,Validade");
                for (Produto p : estoque.getProdutos()) {
                    pw.printf("%d,%s,%d,%.2f,%s%n",
                            p.getId(),
                            p.getNome(),
                            p.getQuantidade(),
                            p.getPreco(),
                            p.getValidade().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    );
                }
                JOptionPane.showMessageDialog(this, "Exportado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarHistoricoCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Salvar histórico como CSV");
        chooser.setSelectedFile(new File("historico.csv"));
        int resp = chooser.showSaveDialog(this);
        if (resp == JFileChooser.APPROVE_OPTION) {
            File arquivo = chooser.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(arquivo)) {
                pw.println("NomeProduto,Quantidade,Tipo,DataHora");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                for (Evento e : estoque.getHistorico()) {
                    pw.printf("%s,%d,%s,%s%n",
                            e.getNomeProduto(),
                            e.getQuantidade(),
                            e.getTipo(),
                            e.getDataHora().format(formatter)
                    );
                }
                JOptionPane.showMessageDialog(this, "Exportado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void salvarEstoque() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .setPrettyPrinting()
                .create();

        try (Writer writer = Files.newBufferedWriter(Paths.get(ARQUIVO_ESTOQUE))) {
            gson.toJson(estoque, writer);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar arquivo: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregarEstoque() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();

        File arquivo = new File(ARQUIVO_ESTOQUE);
        if (arquivo.exists()) {
            try (Reader reader = Files.newBufferedReader(arquivo.toPath())) {
                estoque = gson.fromJson(reader, Estoque.class);
                // Atualiza o proximaId
                proximaId = estoque.getProdutos().stream()
                        .mapToInt(Produto::getId)
                        .max()
                        .orElse(0) + 1;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro ao carregar arquivo: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                estoque = new Estoque();
            }
        } else {
            estoque = new Estoque();
        }
    }

    private void salvarBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Salvar backup JSON");
        chooser.setSelectedFile(new File("backup_estoque.json"));
        int resp = chooser.showSaveDialog(this);
        if (resp == JFileChooser.APPROVE_OPTION) {
            File arquivo = chooser.getSelectedFile();
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .setPrettyPrinting()
                    .create();
            try (Writer writer = Files.newBufferedWriter(arquivo.toPath())) {
                gson.toJson(estoque, writer);
                JOptionPane.showMessageDialog(this, "Backup salvo com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Erro ao salvar backup: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void restaurarBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Abrir backup JSON");
        int resp = chooser.showOpenDialog(this);
        if (resp == JFileChooser.APPROVE_OPTION) {
            File arquivo = chooser.getSelectedFile();
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .create();
            try (Reader reader = Files.newBufferedReader(arquivo.toPath())) {
                estoque = gson.fromJson(reader, Estoque.class);
                proximaId = estoque.getProdutos().stream()
                        .mapToInt(Produto::getId)
                        .max()
                        .orElse(0) + 1;
                salvarEstoque();
                listarProdutos();
                listarHistorico();
                JOptionPane.showMessageDialog(this, "Backup restaurado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro ao restaurar backup: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- CLASSES AUXILIARES PARA DATA ---

    static class DateLabelFormatter extends JFormattedTextField.AbstractFormatter {
        private final String datePattern = "dd/MM/yyyy";
        private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(datePattern);

        @Override
        public Object stringToValue(String text) throws ParseException {
            if (text == null || text.isEmpty()) {
                return null;
            }
            return java.sql.Date.valueOf(LocalDate.parse(text, dateFormatter));
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            if (value != null) {
                java.sql.Date date = (java.sql.Date) value;
                return date.toLocalDate().format(dateFormatter);
            }
            return "";
        }
    }

    // --- ADAPTERS GSON para LocalDate e LocalDateTime ---

    static class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        public JsonElement serialize(LocalDate date, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(date.toString());
        }
        public LocalDate deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) {
            return LocalDate.parse(json.getAsString());
        }
    }

    static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        public JsonElement serialize(LocalDateTime dateTime, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(dateTime.toString());
        }
        public LocalDateTime deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) {
            return LocalDateTime.parse(json.getAsString());
        }
    }

    // --- CLASSES DOMINIO ---

    static class Produto {
        private int id;
        private String nome;
        private int quantidade;
        private double preco;
        private LocalDate validade;

        public Produto(int id, String nome, int quantidade, double preco, LocalDate validade) {
            this.id = id;
            this.nome = nome;
            this.quantidade = quantidade;
            this.preco = preco;
            this.validade = validade;
        }

        public int getId() { return id; }
        public String getNome() { return nome; }
        public int getQuantidade() { return quantidade; }
        public double getPreco() { return preco; }
        public LocalDate getValidade() { return validade; }

        public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
        public void setPreco(double preco) { this.preco = preco; }
        public void setValidade(LocalDate validade) { this.validade = validade; }
    }

    static class Evento {
        private String nomeProduto;
        private int quantidade;
        private String tipo;
        private LocalDateTime dataHora;

        public Evento(String nomeProduto, int quantidade, String tipo, LocalDateTime dataHora) {
            this.nomeProduto = nomeProduto;
            this.quantidade = quantidade;
            this.tipo = tipo;
            this.dataHora = dataHora;
        }

        public String getNomeProduto() { return nomeProduto; }
        public int getQuantidade() { return quantidade; }
        public String getTipo() { return tipo; }
        public LocalDateTime getDataHora() { return dataHora; }
    }

    static class Estoque {
        private List<Produto> produtos;
        private List<Evento> historico;

        public Estoque() {
            produtos = new ArrayList<>();
            historico = new ArrayList<>();
        }

        public List<Produto> getProdutos() { return produtos; }
        public List<Evento> getHistorico() { return historico; }

        public void adicionarProduto(Produto p) {
            produtos.add(p);
        }

        public void adicionarEvento(Evento e) {
            historico.add(e);
        }
    }

    // --- MAIN ---

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Look and Feel padrão do sistema
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new MainGUI().setVisible(true);
        });
    }
}
