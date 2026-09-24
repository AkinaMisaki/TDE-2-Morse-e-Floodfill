package comum;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.imageio.ImageIO;

/**
 * Janela principal do Flood Fill. É uma grade de pixels onde dá pra desenhar com o lápis
 * e clicar com o balde pra preencher. Funciona com qualquer AlgoritmoFloodFill.
 */
public class EditorGrid extends JFrame {

    private static final Color BRANCO = Color.WHITE;
    private static final Color COR_LINHAS = new Color(0xCCCCCC); // Linhas da grade
    private static final int TAMANHO_MAXIMO = 4096;
    private static final Color[] PALETA = {
        Color.BLACK, Color.WHITE, new Color(0x808080), new Color(0xE53935),
        new Color(0xFB8C00), new Color(0xFDD835), new Color(0x43A047), new Color(0x00ACC1),
        new Color(0x1E88E5), new Color(0xA000FF), new Color(0xD81B60), new Color(0x6D4C41)
    };

    private BufferedImage grade;
    private Color corAtual = Color.BLACK;
    private AlgoritmoFloodFill ferramenta; // Null = lápis
    private String nomeFerramenta;
    private volatile boolean executando;
    private Point ultimaCelula;

    private final PainelGrade painel = new PainelGrade();
    private final JPanel barraFerramentas = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private final ButtonGroup grupoFerramentas = new ButtonGroup();
    private final JPanel amostraAtual = new JPanel();
    private final JLabel status = new JLabel("Desenhe com o lapis, escolha um balde e clique num pixel.");
    private final JLabel posicao = new JLabel(" ");
    private final JSpinner spLargura = new JSpinner(new SpinnerNumberModel(32, 1, TAMANHO_MAXIMO, 1));
    private final JSpinner spAltura = new JSpinner(new SpinnerNumberModel(32, 1, TAMANHO_MAXIMO, 1));
    private final JSpinner spPorQuadro = new JSpinner(new SpinnerNumberModel(10, 1, 100000, 1));
    private final JSlider slAtraso = new JSlider(0, 200, 50);       // Atraso em ms por passo
    private final JLabel valorAtraso = new JLabel();
    private final JCheckBox cbSalvarEtapas = new JCheckBox("Salvar etapas em BMP a cada", false);
    private final List<JComponent> controles = new ArrayList<>();
    private final List<String> nomesAlgoritmos = new ArrayList<>();
    private final List<AlgoritmoFloodFill> algoritmos = new ArrayList<>();

    public EditorGrid(int largura, int altura) {
        super("Flood Fill");
        // Fecha só a janela, o menu (Main) continua rodando.
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        spLargura.setValue(largura);
        spAltura.setValue(altura);
        novaGrade(largura, altura);

        JPanel topo = new JPanel();
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        topo.add(criarLinhaFerramentas());
        topo.add(criarLinhaCores());

        add(topo, BorderLayout.NORTH);
        add(painel, BorderLayout.CENTER);
        add(criarRodape(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    /** Cria um botão de balde pro algoritmo (tipo "Pilha" ou "Fila"). */
    public void adicionarAlgoritmo(String nome, AlgoritmoFloodFill algoritmo) {
        nomesAlgoritmos.add(nome);
        algoritmos.add(algoritmo);
        JToggleButton botao = new JToggleButton("Balde (" + nome + ")");
        botao.addActionListener(e -> {
            ferramenta = algoritmo;
            nomeFerramenta = nome;
            painel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        });
        adicionarFerramenta(botao);
    }

    // ---------------------------------------------------------------- Montagem

    private JPanel criarLinhaFerramentas() {
        JToggleButton lapis = new JToggleButton("Lapis", true);
        lapis.addActionListener(e -> {
            ferramenta = null;
            painel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        });
        adicionarFerramenta(lapis);
        painel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        JPanel linha = new JPanel();
        linha.setLayout(new BoxLayout(linha, BoxLayout.Y_AXIS));

        // Primeira linha: ferramentas na esquerda, labirinto na direita.
        JButton labirinto = new JButton("Labirinto infinito...");
        labirinto.addActionListener(e -> abrirLabirinto());
        JPanel direitaTopo = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        direitaTopo.add(labirinto);
        controles.add(labirinto);
        JPanel linhaTopo = new JPanel(new BorderLayout());
        linhaTopo.add(barraFerramentas, BorderLayout.WEST);
        linhaTopo.add(direitaTopo, BorderLayout.EAST);
        linha.add(linhaTopo);

        JPanel direita = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton nova = new JButton("Nova grade");
        nova.addActionListener(e -> {
            novaGrade((Integer) spLargura.getValue(), (Integer) spAltura.getValue());
            status.setText("Nova grade " + grade.getWidth() + "x" + grade.getHeight() + ".");
        });
        JButton limpar = new JButton("Limpar");
        limpar.addActionListener(e -> novaGrade(grade.getWidth(), grade.getHeight()));
        JButton abrir = new JButton("Abrir imagem...");
        abrir.addActionListener(e -> abrirImagem());

        JButton salvar = new JButton("Salvar imagem...");
        salvar.addActionListener(e -> {
            File arquivo = ImageService.salvarComo(this, grade);
            if (arquivo != null) {
                status.setText("Imagem salva em " + arquivo.getAbsolutePath());
            }
        });

        direita.add(abrir);
        direita.add(salvar);
        controles.add(abrir);
        controles.add(salvar);
        direita.add(new JLabel("Largura:"));
        direita.add(spLargura);
        direita.add(new JLabel("Altura:"));
        direita.add(spAltura);
        direita.add(nova);
        direita.add(limpar);
        controles.add(spLargura);
        controles.add(spAltura);
        controles.add(nova);
        controles.add(limpar);
        linha.add(direita);
        return linha;
    }

    private JPanel criarLinhaCores() {
        JPanel linha = new JPanel(new FlowLayout(FlowLayout.LEFT));
        linha.add(new JLabel("Cor:"));
        amostraAtual.setPreferredSize(new Dimension(34, 22));
        amostraAtual.setBackground(corAtual);
        amostraAtual.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        linha.add(amostraAtual);
        linha.add(new JLabel("  "));

        for (Color c : PALETA) {
            linha.add(criarAmostra(c));
        }

        JButton mais = new JButton("Mais cores...");
        mais.addActionListener(e -> {
            Color escolhida = JColorChooser.showDialog(this, "Escolher cor", corAtual);
            if (escolhida != null) {
                setCorAtual(escolhida);
            }
        });
        linha.add(mais);
        controles.add(mais);
        return linha;
    }

    private JPanel criarRodape() {
        JPanel rodape = new JPanel();
        rodape.setLayout(new BoxLayout(rodape, BoxLayout.Y_AXIS));

        // Atraso a cada passo (na fila, cada camada é um passo).
        JPanel opcoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        opcoes.add(new JLabel("Atraso por passo (ms):"));
        slAtraso.setMajorTickSpacing(50);
        slAtraso.setPaintTicks(true);
        slAtraso.setPaintLabels(true);
        opcoes.add(slAtraso);
        valorAtraso.setPreferredSize(new Dimension(45, 20));
        opcoes.add(valorAtraso);
        slAtraso.addChangeListener(e -> atualizarRotuloAtraso());
        atualizarRotuloAtraso();
        controles.add(slAtraso);
        opcoes.add(cbSalvarEtapas);
        opcoes.add(spPorQuadro);
        opcoes.add(new JLabel("pixels"));
        spPorQuadro.setEnabled(false);
        cbSalvarEtapas.addActionListener(e -> spPorQuadro.setEnabled(cbSalvarEtapas.isSelected()));
        controles.add(cbSalvarEtapas);

        JPanel linhaStatus = new JPanel(new BorderLayout());
        linhaStatus.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        linhaStatus.add(status, BorderLayout.CENTER);
        linhaStatus.add(posicao, BorderLayout.EAST);

        rodape.add(opcoes);
        rodape.add(linhaStatus);
        return rodape;
    }

    private void adicionarFerramenta(JToggleButton botao) {
        grupoFerramentas.add(botao);
        barraFerramentas.add(botao);
        controles.add(botao);
        barraFerramentas.revalidate();
    }

    private JPanel criarAmostra(Color cor) {
        JPanel amostra = new JPanel();
        amostra.setPreferredSize(new Dimension(22, 22));
        amostra.setBackground(cor);
        amostra.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        amostra.setToolTipText(String.format("#%06X", cor.getRGB() & 0xFFFFFF));
        amostra.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        amostra.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!executando) {
                    setCorAtual(cor);
                }
            }
        });
        return amostra;
    }

    // ---------------------------------------------------------------- Ações

    /** Abre o labirinto infinito com os mesmos algoritmos desse editor. */
    private void abrirLabirinto() {
        LabirintoInfinito janela = new LabirintoInfinito();
        for (int i = 0; i < algoritmos.size(); i++) {
            janela.adicionarAlgoritmo(nomesAlgoritmos.get(i), algoritmos.get(i));
        }
        janela.setVisible(true);
        janela.iniciar();
    }

    private void setCorAtual(Color cor) {
        corAtual = cor;
        amostraAtual.setBackground(cor);
    }

    private void novaGrade(int largura, int altura) {
        grade = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = grade.createGraphics();
        g.setColor(BRANCO);
        g.fillRect(0, 0, largura, altura);
        g.dispose();
        atualizarRotuloAtraso();
        painel.repaint();
    }

    private void atualizarRotuloAtraso() {
        valorAtraso.setText(slAtraso.getValue() + " ms");
    }

    /** Coloca uma imagem na grade. */
    public void usarImagem(BufferedImage lida) {
        // Copia pra RGB, assim some a transparência e dá pra salvar em BMP.
        BufferedImage copia = new BufferedImage(lida.getWidth(), lida.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copia.createGraphics();
        g.setColor(BRANCO);
        g.fillRect(0, 0, copia.getWidth(), copia.getHeight());
        g.drawImage(lida, 0, 0, null);
        g.dispose();

        grade = copia;
        spLargura.setValue(Math.min(copia.getWidth(), TAMANHO_MAXIMO));
        spAltura.setValue(Math.min(copia.getHeight(), TAMANHO_MAXIMO));
        atualizarRotuloAtraso();
        painel.repaint();
    }

    /** Abre uma imagem escolhida pelo usuário e coloca na grade. */
    private void abrirImagem() {
        JFileChooser seletor = new JFileChooser(new File("."));
        seletor.setDialogTitle("Abrir imagem");
        seletor.setFileFilter(new FileNameExtensionFilter("Imagens (bmp, png, jpg, gif)",
                "bmp", "png", "jpg", "jpeg", "gif"));
        if (seletor.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File arquivo = seletor.getSelectedFile();
        try {
            if (!arquivo.isFile()) {
                throw new IOException("Arquivo nao encontrado: " + arquivo.getName());
            }
            BufferedImage lida = ImageIO.read(arquivo);
            if (lida == null) {
                throw new IOException("Formato de imagem nao suportado: " + arquivo.getName());
            }
            usarImagem(lida);
            status.setText("Imagem " + arquivo.getName() + " aberta (" + grade.getWidth() + "x" + grade.getHeight() + ").");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Nao foi possivel abrir a imagem.\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Pinta as células entre a última posição e a atual, pra não ficar buraco ao arrastar. */
    private void desenharAte(Point celula, Color cor) {
        Point inicio = ultimaCelula != null ? ultimaCelula : celula;
        int passos = Math.max(Math.abs(celula.x - inicio.x), Math.abs(celula.y - inicio.y));
        for (int i = 0; i <= passos; i++) {
            double t = passos == 0 ? 0 : (double) i / passos;
            int x = (int) Math.round(inicio.x + (celula.x - inicio.x) * t);
            int y = (int) Math.round(inicio.y + (celula.y - inicio.y) * t);
            if (x >= 0 && x < grade.getWidth() && y >= 0 && y < grade.getHeight()) {
                grade.setRGB(x, y, cor.getRGB());
            }
        }
        ultimaCelula = celula;
        painel.repaint();
    }

    private void executarFloodFill(int x, int y) {
        AlgoritmoFloodFill algoritmo = ferramenta;
        String nome = nomeFerramenta;
        BufferedImage imagem = grade;
        int cor = corAtual.getRGB() & 0xFFFFFF;
        // Sem a caixa marcada, pasta = null e nada é salvo.
        File pasta = cbSalvarEtapas.isSelected()
                ? new File("saida_" + nome.toLowerCase().replaceAll("[^a-z0-9]+", "_"))
                : null;
        int atraso = slAtraso.getValue();
        int porQuadro = (Integer) spPorQuadro.getValue();

        setExecutando(true);
        status.setText("Executando com " + nome + " a partir de (" + x + ", " + y + ")...");

        // Roda fora da thread da interface, senão a animação não aparece.
        Thread thread = new Thread(() -> {
            String mensagem;
            try {
                ImageService servico = new ImageService(pasta, porQuadro, atraso, painel::repaint);

                long inicio = System.nanoTime();
                algoritmo.executar(imagem, x, y, cor, null, servico);
                long total = System.nanoTime() - inicio;
                long soAlgoritmo = total - servico.getTempoServicoNs();
                int pintados = servico.getPixelsPintados();
                if (pintados == 0) {
                    mensagem = "A cor escolhida e igual a cor do pixel (" + x + ", " + y + "). Nada a fazer.";
                } else {
                    mensagem = nome + ": " + pintados + " pixels em " + servico.getPassos() + " passos, "
                            + ImageService.formatarTempo(total) + " (algoritmo: "
                            + ImageService.formatarTempo(soAlgoritmo) + ", " + atraso + " ms/passo)"
                            + (pasta == null ? "." : ", " + servico.getQuadrosSalvos()
                               + " imagens salvas em " + pasta.getAbsolutePath());
                }
            } catch (IOException | RuntimeException ex) {
                mensagem = "Erro: " + ex.getMessage();
            }
            String texto = mensagem;
            SwingUtilities.invokeLater(() -> {
                setExecutando(false);
                status.setText(texto);
                painel.repaint();
            });
        }, "flood-fill");
        thread.setDaemon(true);
        thread.start();
    }

    private void setExecutando(boolean valor) {
        executando = valor;
        for (JComponent c : controles) {
            c.setEnabled(!valor);
        }
        spPorQuadro.setEnabled(!valor && cbSalvarEtapas.isSelected());
    }

    // ---------------------------------------------------------------- Painel da grade

    private class PainelGrade extends JPanel {

        PainelGrade() {
            setPreferredSize(new Dimension(640, 640));
            setBackground(new Color(0x2B2B2B));

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Point celula = celulaEm(e.getX(), e.getY());
                    if (executando || celula == null) {
                        return;
                    }
                    if (ferramenta == null) {
                        ultimaCelula = null;
                        desenharAte(celula, SwingUtilities.isRightMouseButton(e) ? BRANCO : corAtual);
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        executarFloodFill(celula.x, celula.y);
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    atualizarPosicao(e);
                    if (executando || ferramenta != null) {
                        return;
                    }
                    Point celula = celulaEm(e.getX(), e.getY());
                    if (celula != null) {
                        desenharAte(celula, SwingUtilities.isRightMouseButton(e) ? BRANCO : corAtual);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    ultimaCelula = null;
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    atualizarPosicao(e);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    posicao.setText(" ");
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            setToolTipText(null);
        }

        private void atualizarPosicao(MouseEvent e) {
            Point celula = celulaEm(e.getX(), e.getY());
            posicao.setText(celula == null ? " " : "X = " + celula.x + ", Y = " + celula.y);
        }

        /** Tamanho de cada pixel na tela (inteiro quando dá, pros pixels ficarem nítidos). */
        private double tamanhoCelula() {
            double escala = Math.min((double) getWidth() / grade.getWidth(),
                    (double) getHeight() / grade.getHeight());
            return escala >= 1 ? Math.floor(escala) : escala;
        }

        private int origemX() {
            return (int) ((getWidth() - tamanhoCelula() * grade.getWidth()) / 2);
        }

        private int origemY() {
            return (int) ((getHeight() - tamanhoCelula() * grade.getHeight()) / 2);
        }

        /** Converte a posição do mouse em coordenada da grade (null se estiver fora). */
        Point celulaEm(int mx, int my) {
            double tam = tamanhoCelula();
            int dx = mx - origemX();
            int dy = my - origemY();
            if (dx < 0 || dy < 0) {
                return null;
            }
            int x = (int) (dx / tam);
            int y = (int) (dy / tam);
            if (x >= grade.getWidth() || y >= grade.getHeight()) {
                return null;
            }
            return new Point(x, y);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            double escala = tamanhoCelula();
            int ox = origemX();
            int oy = origemY();
            int w = (int) (grade.getWidth() * escala);
            int h = (int) (grade.getHeight() * escala);

            g.drawImage(grade, ox, oy, w, h, null);

            if (escala >= 6) {
                int tam = (int) escala;
                g.setColor(COR_LINHAS);
                for (int i = 0; i <= grade.getWidth(); i++) {
                    g.drawLine(ox + i * tam, oy, ox + i * tam, oy + h);
                }
                for (int j = 0; j <= grade.getHeight(); j++) {
                    g.drawLine(ox, oy + j * tam, ox + w, oy + j * tam);
                }
            }
        }
    }
}
