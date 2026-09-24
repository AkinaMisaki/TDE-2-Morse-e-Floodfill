package comum;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Cuida das etapas do Flood Fill: pinta, anima e salva os passos em BMP (passo_0000.bmp, ...).
 * As versões com pilha e fila usam ele do mesmo jeito.
 */
public class ImageService {

    private final File pastaSaida;
    private final int pixelsPorQuadro;
    private static final double QUADRO_MS = 15;
    private static final double ATRASO_MAXIMO_MS = 250;

    private final double atrasoMs;
    private final Runnable aoPintar;

    private int numeroQuadro;
    private int pixelsPintados;
    private long tempoServicoNs;
    private long inicioRitmoNs;
    private long passos;
    private int ultimaDistancia;

    /**
     * @param pastaSaida      Onde salvar as etapas (null = não salva)
     * @param pixelsPorQuadro Salva um BMP a cada N pixels pintados
     * @param atrasoMs        Pausa entre os passos da animação (0 = sem pausa)
     * @param aoPintar        Roda a cada pixel pintado (tipo um repaint), pode ser null
     */
    public ImageService(File pastaSaida, int pixelsPorQuadro, double atrasoMs, Runnable aoPintar) {
        if (pixelsPorQuadro < 1) {
            throw new IllegalArgumentException("pixelsPorQuadro deve ser >= 1");
        }
        this.pastaSaida = pastaSaida;
        this.pixelsPorQuadro = pixelsPorQuadro;
        this.atrasoMs = Math.max(0, atrasoMs);
        this.aoPintar = aoPintar;
    }

    /** Prepara a pasta e salva a imagem original (passo_0000). */
    public void iniciar(BufferedImage imagem) throws IOException {
        long t0 = System.nanoTime();
        numeroQuadro = 0;
        pixelsPintados = 0;
        tempoServicoNs = 0;
        passos = 0;
        ultimaDistancia = -1;
        prepararPasta();
        salvarQuadro(imagem);
        tempoServicoNs += System.nanoTime() - t0;
    }

    /**
     * Pinta p com a cor e cuida da animação, do contador e dos BMPs.
     * O ritmo é por passo, não por pixel: só espera quando a distância até o início muda.
     * Na fila cada camada aparece de uma vez, na pilha fica quase um pixel por passo.
     */
    public void pintar(BufferedImage imagem, Position p, int cor) throws IOException {
        long t0 = System.nanoTime();
        if (p.distancia != ultimaDistancia) {
            ultimaDistancia = p.distancia;
            if (atrasoMs > 0) {
                esperarVez(); // Espera antes de pintar, pro pixel não aparecer adiantado
            } else {
                passos++;
            }
        }
        imagem.setRGB(p.col, p.row, cor);
        pixelsPintados++;
        if (pixelsPintados % pixelsPorQuadro == 0) {
            salvarQuadro(imagem);
        }
        if (aoPintar != null) {
            aoPintar.run();
        }
        tempoServicoNs += System.nanoTime() - t0;
    }

    /** Salva a imagem final, se o último quadro ainda não foi salvo. */
    public void finalizar(BufferedImage imagem) throws IOException {
        long t0 = System.nanoTime();
        if (pixelsPintados % pixelsPorQuadro != 0) {
            salvarQuadro(imagem);
        }
        if (aoPintar != null) {
            aoPintar.run();
        }
        tempoServicoNs += System.nanoTime() - t0;
    }

    /**
     * Segura o ritmo da animação: o passo N acontece em início + N * atraso.
     * Só dorme se estiver adiantado pelo menos ~15 ms, que é o mínimo que o Windows consegue.
     */
    private void esperarVez() {
        if (atrasoMs <= 0) {
            return;
        }
        long agora = System.nanoTime();
        if (passos == 0) {
            inicioRitmoNs = agora;
            passos = 1;
            return;
        }
        long alvo = inicioRitmoNs + (long) (passos * atrasoMs * 1_000_000);
        passos++;
        double adiantadoMs = (alvo - agora) / 1_000_000.0;
        if (adiantadoMs >= QUADRO_MS) {
            try {
                Thread.sleep((long) adiantadoMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else if (adiantadoMs < -ATRASO_MAXIMO_MS) {
            // Muito atrasado (tipo depois de pausar), então zera o relógio pra não sair correndo.
            inicioRitmoNs = agora;
            passos = 1;
        }
    }

    public int getPixelsPintados() {
        return pixelsPintados;
    }

    /** Quantos passos a animação teve (na fila, cada camada é um passo). */
    public long getPassos() {
        return passos;
    }

    /** Tempo gasto no serviço (pausas, tela e BMPs). Tirando do total, sobra só o do algoritmo. */
    public long getTempoServicoNs() {
        return tempoServicoNs;
    }

    /** Formata uma duração, tipo "0,42 ms", "35 ms" ou "2,31 s". */
    public static String formatarTempo(long nanos) {
        double ms = nanos / 1_000_000.0;
        if (ms < 10) {
            return String.format("%.2f ms", ms);
        }
        if (ms < 1000) {
            return String.format("%.0f ms", ms);
        }
        return String.format("%.2f s", ms / 1000);
    }

    public int getQuadrosSalvos() {
        return numeroQuadro;
    }

    private void prepararPasta() throws IOException {
        if (pastaSaida == null) {
            return;
        }
        if (!pastaSaida.exists() && !pastaSaida.mkdirs()) {
            throw new IOException("Nao foi possivel criar a pasta " + pastaSaida.getPath());
        }
        // Apaga as etapas antigas pra não misturar os resultados.
        File[] antigos = pastaSaida.listFiles((d, nome) -> nome.startsWith("passo_") && nome.endsWith(".bmp"));
        if (antigos != null) {
            for (File f : antigos) {
                f.delete();
            }
        }
    }

    private void salvarQuadro(BufferedImage imagem) throws IOException {
        if (pastaSaida == null) {
            return; // Modo sem salvar
        }
        File arquivo = new File(pastaSaida, String.format("passo_%04d.bmp", numeroQuadro++));
        // O BMP do ImageIO não aceita canal alfa, então garante RGB.
        BufferedImage rgb = imagem.getType() == BufferedImage.TYPE_INT_RGB ? imagem : paraRgb(imagem);
        if (!ImageIO.write(rgb, "bmp", arquivo)) {
            throw new IOException("Falha ao salvar " + arquivo.getPath());
        }
    }

    /**
     * Abre um "Salvar como" e grava uma cópia da imagem em BMP ou PNG (o padrão é PNG).
     *
     * @return O arquivo salvo, ou null se cancelar ou der erro
     */
    public static File salvarComo(Component pai, BufferedImage imagem) {
        BufferedImage copia = paraRgb(imagem); // Copia agora, antes da imagem mudar
        JFileChooser seletor = new JFileChooser(new File("."));
        seletor.setDialogTitle("Salvar imagem");
        FileNameExtensionFilter png = new FileNameExtensionFilter("PNG (*.png)", "png");
        FileNameExtensionFilter bmp = new FileNameExtensionFilter("BMP (*.bmp)", "bmp");
        seletor.addChoosableFileFilter(png);
        seletor.addChoosableFileFilter(bmp);
        seletor.setFileFilter(png);
        seletor.setSelectedFile(new File("imagem.png"));
        if (seletor.showSaveDialog(pai) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File arquivo = seletor.getSelectedFile();
        String nome = arquivo.getName().toLowerCase();
        String formato;
        if (nome.endsWith(".bmp")) {
            formato = "bmp";
        } else if (nome.endsWith(".png")) {
            formato = "png";
        } else {
            formato = seletor.getFileFilter() == bmp ? "bmp" : "png";
            arquivo = new File(arquivo.getParentFile(), arquivo.getName() + "." + formato);
        }

        try {
            if (!ImageIO.write(copia, formato, arquivo)) {
                throw new IOException("formato nao suportado");
            }
            return arquivo;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(pai, "Nao foi possivel salvar a imagem.\n" + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    /** Copia a imagem pra RGB, porque o BMP não aceita canal alfa (o transparente vira branco). */
    private static BufferedImage paraRgb(BufferedImage src) {
        BufferedImage copia = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copia.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, copia.getWidth(), copia.getHeight());
        g.drawImage(src, 0, 0, null); // Copia a imagem inteira de uma vez, bem mais rápido que pixel a pixel
        g.dispose();
        return copia;
    }
}
