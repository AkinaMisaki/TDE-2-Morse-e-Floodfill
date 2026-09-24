import comum.AlgoritmoFloodFill;
import comum.EditorGrid;
import fila.FloodFillFila;
import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;

/**
 * Começo do projeto Flood Fill. Abre o editor gráfico com as versões de pilha e fila.
 *
 * Pastas:  comum/  Coisas que as duas versões usam
 *          fila/   FloodFillFila
 *          pilha/  FloodFillPilha
 *
 * Pra rodar, na pasta Floodfill: javac Main.java e depois java Main
 */
public class Main {

    /** Versão com pilha. Quando o FloodFillPilha existir, é só trocar o null por new pilha.FloodFillPilha(). */
    private static AlgoritmoFloodFill criarPilha() {
        return null;
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Este projeto precisa de interface grafica.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            EditorGrid editor = new EditorGrid(32, 32);
            AlgoritmoFloodFill pilha = criarPilha();
            if (pilha != null) {
                editor.adicionarAlgoritmo("Pilha", pilha);
            }
            editor.adicionarAlgoritmo("Fila", new FloodFillFila());
            editor.setVisible(true);
        });
    }
}
