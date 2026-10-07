import comum.EditorGrid;
import fila.FloodFillFila;
import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import pilha.FloodFillPilha;

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

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Este projeto precisa de interface grafica.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            EditorGrid editor = new EditorGrid(32, 32);
            editor.adicionarAlgoritmo("Pilha", new FloodFillPilha());
            editor.adicionarAlgoritmo("Fila", new FloodFillFila());
            editor.setVisible(true);
        });
    }
}
