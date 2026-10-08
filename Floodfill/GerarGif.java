import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/**
 * Junta as etapas salvas pelo Flood Fill (passo_0000.bmp, passo_0001.bmp, ...) num GIF animado.
 * O GIF fica do lado da pasta, com o mesmo nome dela (saida_fila -> saida_fila.gif).
 *
 * Pra rodar, na pasta Floodfill: javac GerarGif.java e depois
 *     java GerarGif                      Converte todas as pastas saida_* daqui
 *     java GerarGif saida_fila           Converte só essa pasta
 *     java GerarGif saida_fila 30 500    30 ms por quadro e no máximo 500 quadros
 */
public class GerarGif {

    private static final int MS_PADRAO = 50;
    private static final int MAXIMO_PADRAO = 300;
    private static final int PAUSA_FINAL_MS = 2000; // O último quadro fica parado um pouco antes de repetir

    public static void main(String[] args) {
        int ms = MS_PADRAO;
        int maximo = MAXIMO_PADRAO;
        try {
            if (args.length > 1) {
                ms = Integer.parseInt(args[1]);
            }
            if (args.length > 2) {
                maximo = Integer.parseInt(args[2]);
            }
        } catch (NumberFormatException e) {
            System.out.println("O tempo por quadro e o maximo de quadros precisam ser numeros inteiros.");
            mostrarUso();
            return;
        }
        // Abaixo de 20 ms os navegadores ignoram o tempo e mostram o GIF devagar.
        if (ms < 20 || maximo < 2) {
            System.out.println("Use pelo menos 20 ms por quadro e pelo menos 2 quadros.");
            mostrarUso();
            return;
        }

        File[] pastas;
        if (args.length > 0) {
            pastas = new File[] {new File(args[0])};
        } else {
            pastas = new File(".").listFiles(f -> f.isDirectory() && f.getName().startsWith("saida_"));
            if (pastas == null || pastas.length == 0) {
                System.out.println("Nenhuma pasta saida_* aqui. Rode o Flood Fill com \"Salvar etapas em BMP\" marcado.");
                mostrarUso();
                return;
            }
        }

        for (File pasta : pastas) {
            try {
                gerar(pasta.getCanonicalFile(), ms, maximo);
            } catch (IOException e) {
                System.out.println("Erro em " + pasta.getPath() + ": " + e.getMessage());
            }
        }
    }

    private static void mostrarUso() {
        System.out.println("Uso: java GerarGif [pasta] [ms por quadro] [maximo de quadros]");
        System.out.println("     (padrao: todas as pastas saida_*, " + MS_PADRAO + " ms, " + MAXIMO_PADRAO + " quadros)");
    }

    /** Junta os passo_*.bmp da pasta num GIF animado que repete pra sempre. */
    private static void gerar(File pasta, int ms, int maximo) throws IOException {
        if (!pasta.isDirectory()) {
            throw new IOException("pasta nao encontrada");
        }
        File[] etapas = pasta.listFiles((d, nome) -> nome.matches("passo_\\d+\\.bmp"));
        if (etapas == null || etapas.length == 0) {
            throw new IOException("nenhum passo_*.bmp na pasta");
        }
        // Ordena pelo número, porque pelo nome o passo_10000 viria antes do passo_9999.
        Arrays.sort(etapas, Comparator.comparingInt(GerarGif::numero));

        // Com etapas demais, usa só algumas, espalhadas por igual (sempre com a primeira e a última).
        int quadros = Math.min(etapas.length, maximo);

        File destino = new File(pasta.getParentFile(), pasta.getName() + ".gif");
        Files.deleteIfExists(destino.toPath()); // Senão sobra lixo do GIF antigo no fim do arquivo
        ImageWriter escritor = ImageIO.getImageWritersBySuffix("gif").next();
        try (ImageOutputStream saida = ImageIO.createImageOutputStream(destino)) {
            escritor.setOutput(saida);
            escritor.prepareWriteSequence(null);
            int largura = 0;
            int altura = 0;
            for (int i = 0; i < quadros; i++) {
                File etapa = etapas[quadros == 1 ? 0 : (int) ((long) i * (etapas.length - 1) / (quadros - 1))];
                BufferedImage imagem = ImageIO.read(etapa);
                if (imagem == null) {
                    throw new IOException("nao consegui ler " + etapa.getName());
                }
                // Como as etapas acumulam, a pasta pode ter sobras de outra imagem. O GIF precisa de um tamanho só.
                if (i == 0) {
                    largura = imagem.getWidth();
                    altura = imagem.getHeight();
                } else if (imagem.getWidth() != largura || imagem.getHeight() != altura) {
                    throw new IOException(etapa.getName() + " tem " + imagem.getWidth() + "x" + imagem.getHeight()
                            + ", mas as etapas anteriores tem " + largura + "x" + altura
                            + ". Use \"Resetar BMPs\" no editor antes de trocar de imagem.");
                }
                IIOMetadata dados = escritor.getDefaultImageMetadata(
                        ImageTypeSpecifier.createFromRenderedImage(imagem), null);
                configurar(dados, i == quadros - 1 ? PAUSA_FINAL_MS : ms, i == 0);
                escritor.writeToSequence(new IIOImage(imagem, null, dados), null);
            }
            escritor.endWriteSequence();
        } catch (IOException e) {
            destino.delete(); // Não deixa um GIF pela metade
            throw e;
        } finally {
            escritor.dispose();
        }

        System.out.println(destino.getPath() + ": " + quadros + " quadros (de " + etapas.length + " etapas), "
                + ms + " ms por quadro, " + (destino.length() + 1023) / 1024 + " KB");
    }

    /** O número da etapa, tirado do nome (passo_0042.bmp -> 42). */
    private static int numero(File etapa) {
        String nome = etapa.getName();
        return Integer.parseInt(nome.substring("passo_".length(), nome.length() - ".bmp".length()));
    }

    /** Define o tempo do quadro e, no primeiro quadro, que a animação repete pra sempre. */
    private static void configurar(IIOMetadata dados, int ms, boolean primeiro) throws IOException {
        String formato = dados.getNativeMetadataFormatName();
        IIOMetadataNode raiz = (IIOMetadataNode) dados.getAsTree(formato);

        IIOMetadataNode controle = filho(raiz, "GraphicControlExtension");
        controle.setAttribute("disposalMethod", "none");
        controle.setAttribute("userInputFlag", "FALSE");
        controle.setAttribute("transparentColorFlag", "FALSE");
        controle.setAttribute("transparentColorIndex", "0");
        controle.setAttribute("delayTime", String.valueOf(ms / 10)); // O GIF conta em centésimos de segundo

        if (primeiro) {
            IIOMetadataNode repetir = new IIOMetadataNode("ApplicationExtension");
            repetir.setAttribute("applicationID", "NETSCAPE");
            repetir.setAttribute("authenticationCode", "2.0");
            repetir.setUserObject(new byte[] {1, 0, 0}); // Os dois zeros são o número de repetições: 0 = pra sempre
            filho(raiz, "ApplicationExtensions").appendChild(repetir);
        }
        dados.setFromTree(formato, raiz);
    }

    /** Acha o nó com esse nome dentro da raiz, ou cria se não existir. */
    private static IIOMetadataNode filho(IIOMetadataNode raiz, String nome) {
        for (int i = 0; i < raiz.getLength(); i++) {
            if (raiz.item(i).getNodeName().equals(nome)) {
                return (IIOMetadataNode) raiz.item(i);
            }
        }
        IIOMetadataNode novo = new IIOMetadataNode(nome);
        raiz.appendChild(novo);
        return novo;
    }
}
