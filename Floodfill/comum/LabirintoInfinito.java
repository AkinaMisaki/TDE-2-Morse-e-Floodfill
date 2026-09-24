package comum;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/**
 * Fica gerando labirintos aleatórios e resolvendo cada um com o Flood Fill, da entrada até a saída.
 * A área explorada fica vermelha e o caminho encontrado fica verde.
 */
public class LabirintoInfinito extends JFrame {

    private static final int PAREDE = 0x000000;
    private static final int LIVRE = 0xFFFFFF;
    private static final int EXPLORADO = 0xE53935; // Vermelho
    private static final int TAMANHO_MAXIMO = 99; // Ímpar, porque o lado do labirinto tem que ser ímpar
    private static final int CAMINHO = 0x00C853;
    private static final String ALTERNAR = "Alternar";

    private final List<String> nomes = new ArrayList<>();
    private final List<AlgoritmoFloodFill> algoritmos = new ArrayList<>();
    private final Random aleatorio = new Random();

    private volatile BufferedImage imagem;
    private volatile boolean pausado;
    private volatile boolean ativo = true;
    private volatile int tamanho = 41;
    private volatile int atrasoMs;       // Começa com o valor do slider (ver construtor)
    private volatile String escolhido;
    private int resolvidos;
    private volatile long tempoPausadoNs;

    private final PainelLabirinto painel = new PainelLabirinto();
    private final JComboBox<String> cbAlgoritmo = new JComboBox<>();
    private final JSpinner spTamanho = new JSpinner(new SpinnerNumberModel(41, 5, TAMANHO_MAXIMO, 1));
    private final JSlider slAtraso = new JSlider(0, 50, 15);
    private final JToggleButton btPausar = new JToggleButton("Pausar");
    private final JLabel status = new JLabel("Gerando o primeiro labirinto...");

    public LabirintoInfinito() {
        super("Labirinto Infinito - Flood Fill");
        atrasoMs = slAtraso.getValue(); // Assim o atraso usado é sempre o que aparece na tela
        // Fecha só essa janela e para o laço dos labirintos.
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                ativo = false;
                pausado = false;
                synchronized (LabirintoInfinito.this) {
                    LabirintoInfinito.this.notifyAll();
                }
            }
        });

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Algoritmo:"));
        topo.add(cbAlgoritmo);
        topo.add(new JLabel("  Tamanho:"));
        topo.add(spTamanho);
        topo.add(new JLabel("(máx. " + TAMANHO_MAXIMO + ")"));
        topo.add(btPausar);
        JButton salvar = new JButton("Salvar imagem...");
        topo.add(salvar);

        JPanel rodape = new JPanel();
        rodape.setLayout(new BoxLayout(rodape, BoxLayout.Y_AXIS));
        JPanel opcoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        opcoes.add(new JLabel("Atraso por passo (ms):"));
        slAtraso.setMajorTickSpacing(10);
        slAtraso.setPaintTicks(true);
        slAtraso.setPaintLabels(true);
        opcoes.add(slAtraso);
        JLabel valorAtraso = new JLabel(slAtraso.getValue() + " ms");
        valorAtraso.setPreferredSize(new Dimension(50, 20));
        opcoes.add(valorAtraso);
        JPanel linhaStatus = new JPanel(new BorderLayout());
        linhaStatus.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        linhaStatus.add(status, BorderLayout.CENTER);
        rodape.add(opcoes);
        rodape.add(linhaStatus);

        // Os controles só mudam campos voláteis, quem lê é a thread do labirinto.
        spTamanho.addChangeListener(e -> tamanho = (Integer) spTamanho.getValue());
        slAtraso.addChangeListener(e -> {
            atrasoMs = slAtraso.getValue();
            valorAtraso.setText(atrasoMs + " ms");
        });
        cbAlgoritmo.addActionListener(e -> escolhido = (String) cbAlgoritmo.getSelectedItem());
        btPausar.addActionListener(e -> {
            pausado = btPausar.isSelected();
            btPausar.setText(pausado ? "Continuar" : "Pausar");
            synchronized (this) {
                notifyAll();
            }
        });
        salvar.addActionListener(e -> {
            BufferedImage atual = imagem;
            if (atual != null) {
                File arquivo = ImageService.salvarComo(this, atual);
                if (arquivo != null) {
                    status.setText("Imagem salva em " + arquivo.getAbsolutePath());
                }
            }
        });

        add(topo, BorderLayout.NORTH);
        add(painel, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    /** Adiciona um algoritmo (tipo "Fila" ou "Pilha"). Chamar antes do iniciar(). */
    public void adicionarAlgoritmo(String nome, AlgoritmoFloodFill algoritmo) {
        nomes.add(nome);
        algoritmos.add(algoritmo);
        cbAlgoritmo.removeAllItems();
        for (String n : nomes) {
            cbAlgoritmo.addItem(n);
        }
        if (nomes.size() > 1) {
            cbAlgoritmo.addItem(ALTERNAR);
        }
        cbAlgoritmo.setSelectedIndex(0);
        escolhido = nomes.get(0);
    }

    /** Liga a thread que gera e resolve labirintos sem parar. */
    public void iniciar() {
        Thread thread = new Thread(this::laco, "labirinto-infinito");
        thread.setDaemon(true);
        thread.start();
    }

    // ---------------------------------------------------------------- Laço principal

    private void laco() {
        int vez = 0;
        while (ativo) {
            esperarSePausado();

            int lado = tamanho % 2 == 0 ? tamanho - 1 : tamanho; // O lado tem que ser ímpar
            BufferedImage lab = gerar(lado, lado);
            Position entrada = new Position(0, 1);
            Position saida = new Position(lado - 1, lado - 2);
            imagem = lab;
            painel.repaint();
            dormir(400);

            int indice = indiceDoAlgoritmo(vez++);
            String nome = nomes.get(indice);
            atualizarStatus("Resolvendo labirinto " + lado + "x" + lado + " com " + nome + "...");

            int atraso = atrasoMs;
            ImageService servico = new ImageService(null, 1, atraso, () -> {
                if (!ativo) {
                    throw new JanelaFechada(); // Janela fechada: interrompe o labirinto na hora
                }
                painel.repaint();
                esperarSePausado();
            });

            try {
                long pausadoAntes = tempoPausadoNs;
                long inicio = System.nanoTime();
                Position chegada = algoritmos.get(indice).executar(
                        lab, entrada.col, entrada.row, EXPLORADO, saida, servico);
                long decorrido = System.nanoTime() - inicio;
                // Tempo até a saída, sem contar o tempo pausado.
                long total = decorrido - (tempoPausadoNs - pausadoAntes);
                // A pausa já está dentro do serviço, então sai junto aqui.
                long soAlgoritmo = decorrido - servico.getTempoServicoNs();
                int explorados = servico.getPixelsPintados();
                if (chegada != null) {
                    int passos = destacarCaminho(lab, chegada, servico);
                    resolvidos++;
                    atualizarStatus(nome + ": caminho " + passos + " px, " + explorados + " explorados em "
                            + ImageService.formatarTempo(total) + " (algoritmo: "
                            + ImageService.formatarTempo(soAlgoritmo) + ", " + atraso
                            + " ms/passo). Resolvidos: " + resolvidos);
                } else {
                    atualizarStatus(nome + ": a saida nao foi alcancada.");
                }
            } catch (JanelaFechada e) {
                return;
            } catch (IOException | RuntimeException e) {
                atualizarStatus("Erro: " + e.getMessage());
            }

            painel.repaint();
            dormir(1500);
        }
    }

    private int indiceDoAlgoritmo(int vez) {
        if (ALTERNAR.equals(escolhido)) {
            return vez % algoritmos.size();
        }
        int i = nomes.indexOf(escolhido);
        return i < 0 ? 0 : i;
    }

    /**
     * Segue os anteriores da saída até a entrada e pinta o caminho, com animação.
     *
     * @return Quantos pixels o caminho tem
     */
    private int destacarCaminho(BufferedImage lab, Position chegada, ImageService servico) throws IOException {
        int total = 0;
        for (Position p = chegada; p != null; p = p.anterior) {
            total++;
        }
        Position[] caminho = new Position[total];
        int i = total - 1;
        for (Position p = chegada; p != null; p = p.anterior) {
            caminho[i--] = p;
        }
        for (Position p : caminho) {
            servico.pintar(lab, p, CAMINHO); // Um pixel por passo, no mesmo ritmo
        }
        return total;
    }

    /**
     * Gera um labirinto aleatório com busca em profundidade (pilha num vetor).
     * Células nas posições ímpares, paredes nas pares, entrada em cima e saída embaixo.
     */
    private BufferedImage gerar(int largura, int altura) {
        BufferedImage lab = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = lab.createGraphics();
        g.setColor(new Color(PAREDE));
        g.fillRect(0, 0, largura, altura);
        g.dispose();

        int colunas = (largura - 1) / 2;
        int linhas = (altura - 1) / 2;
        boolean[] visitado = new boolean[colunas * linhas];
        int[] pilha = new int[colunas * linhas];
        int topo = 0;
        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};
        int[] opcoes = new int[4]; // Criado uma vez só, reaproveitado a cada volta

        visitado[0] = true;
        pilha[topo++] = 0;
        lab.setRGB(1, 1, LIVRE);

        while (topo > 0) {
            int atual = pilha[topo - 1];
            int cx = atual % colunas;
            int cy = atual / colunas;

            // Junta os vizinhos ainda não visitados.
            int n = 0;
            for (int d = 0; d < 4; d++) {
                int nx = cx + dx[d];
                int ny = cy + dy[d];
                if (nx >= 0 && nx < colunas && ny >= 0 && ny < linhas && !visitado[ny * colunas + nx]) {
                    opcoes[n++] = d;
                }
            }
            if (n == 0) {
                topo--; // Beco sem saída, então volta
                continue;
            }

            // Sorteia um vizinho e derruba a parede entre os dois.
            int d = opcoes[aleatorio.nextInt(n)];
            int nx = cx + dx[d];
            int ny = cy + dy[d];
            lab.setRGB(2 * cx + 1 + dx[d], 2 * cy + 1 + dy[d], LIVRE);
            lab.setRGB(2 * nx + 1, 2 * ny + 1, LIVRE);
            visitado[ny * colunas + nx] = true;
            pilha[topo++] = ny * colunas + nx;
        }

        lab.setRGB(1, 0, LIVRE);                    // Entrada
        lab.setRGB(largura - 2, altura - 1, LIVRE); // Saída
        return lab;
    }

    // ---------------------------------------------------------------- Utilitários

    private synchronized void esperarSePausado() {
        if (!pausado) {
            return;
        }
        long inicio = System.nanoTime();
        try {
            aguardarContinuar();
        } finally {
            tempoPausadoNs += System.nanoTime() - inicio;
        }
    }

    private void aguardarContinuar() {
        while (pausado) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void dormir(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void atualizarStatus(String texto) {
        SwingUtilities.invokeLater(() -> status.setText(texto));
    }

    /** Usada pra parar o algoritmo no meio quando a janela é fechada. */
    private static class JanelaFechada extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private class PainelLabirinto extends JPanel {

        PainelLabirinto() {
            setPreferredSize(new Dimension(640, 640));
            setBackground(new Color(0x2B2B2B));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            BufferedImage atual = imagem;
            if (atual == null) {
                return;
            }
            double escala = Math.min((double) getWidth() / atual.getWidth(),
                    (double) getHeight() / atual.getHeight());
            if (escala >= 1) {
                escala = Math.floor(escala); // Escala inteira pros pixels ficarem nítidos
            }
            int w = (int) (atual.getWidth() * escala);
            int h = (int) (atual.getHeight() * escala);
            g.drawImage(atual, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
        }
    }
}
