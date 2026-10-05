import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Menu dos dois projetos do TDE 2. Mostra um menu, compila o projeto escolhido e roda ele.
 * Cada projeto roda num processo separado porque as duas pastas têm uma classe Main.
 *
 * Pra rodar, na raiz do repositório: java Main.java
 */
public class Main {

    /** Entrada padrão sem buffer, porque o System.in lê adiantado. */
    private static final FileInputStream entrada = new FileInputStream(FileDescriptor.in);

    /**
     * Lê uma linha do teclado byte por byte, pra não "roubar" linhas do menu do projeto (outro processo).
     *
     * @return A linha sem espaços nas pontas, ou null se a entrada acabou
     */
    private static String lerLinha() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            int b;
            while ((b = entrada.read()) != -1 && b != '\n') {
                if (b != '\r') {
                    bytes.write(b);
                }
            }
            if (b == -1 && bytes.size() == 0) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        return bytes.toString().trim();
    }

    public static void main(String[] args) {
        while (true) {
            System.out.println();
            System.out.println("============= TDE 2 =============");
            System.out.println("1 - Código Morse");
            System.out.println("2 - Flood Fill");
            System.out.println("0 - Encerrar");
            System.out.print("Opção: ");
            String opcao = lerLinha();
            if (opcao == null) {
                return; // A entrada acabou
            }
            switch (opcao) {
                case "1":
                    abrirProjeto("Morse");
                    break;
                case "2":
                    abrirProjeto("Floodfill");
                    break;
                case "0":
                    System.out.println("Encerrando.");
                    return;
                default:
                    System.out.println("Opção inválida: \"" + opcao + "\". Digite 1, 2 ou 0.");
            }
        }
    }

    /** Compila a pasta do projeto e roda o Main dela no mesmo console. */
    private static void abrirProjeto(String pasta) {
        File dir = new File(pasta);
        if (!new File(dir, "Main.java").isFile()) {
            System.out.println("Não encontrei " + pasta + "/Main.java. Rode este lançador na raiz do repositório.");
            return;
        }

        System.out.println("Compilando " + pasta + "...");
        List<String> compilar = new ArrayList<>();
        compilar.add(ferramenta("javac"));
        File[] fontes = dir.listFiles((d, nome) -> nome.endsWith(".java"));
        if (fontes != null) {
            for (File f : fontes) {
                compilar.add(f.getName());
            }
        }
        if (executar(compilar, dir) != 0) {
            System.out.println("Erro de compilação em " + pasta + ". Corrija e tente de novo.");
            return;
        }
        // Compilou, mas não tem classe Main (tipo um Main.java ainda vazio).
        if (!new File(dir, "Main.class").isFile()) {
            System.out.println(pasta + " ainda não tem um menu: falta a classe Main (arquivo Main.java).");
            return;
        }

        System.out.println();
        int codigo = executar(List.of(ferramenta("java"), "-cp", ".", "Main"), dir);
        System.out.println();
        System.out.println(pasta + " encerrado" + (codigo == 0 ? "." : " (codigo " + codigo + ").")
                + " Voltando ao menu principal.");
    }

    /** Roda um comando na pasta, ligado nesse console, e espera terminar. */
    private static int executar(List<String> comando, File dir) {
        try {
            Process processo = new ProcessBuilder(comando).directory(dir).inheritIO().start();
            return processo.waitFor();
        } catch (IOException e) {
            System.out.println("Não foi possível executar " + comando.get(0) + ": " + e.getMessage());
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    /** Usa o java/javac do mesmo JDK do lançador, ou o do PATH se não achar. */
    private static String ferramenta(String nome) {
        String exe = System.getProperty("os.name").toLowerCase().contains("win") ? nome + ".exe" : nome;
        File local = new File(System.getProperty("java.home"), "bin" + File.separator + exe);
        return local.isFile() ? local.getAbsolutePath() : nome;
    }
}
