package comum;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Interface que as versões com pilha e fila seguem, assim o EditorGrid aceita qualquer uma.
 */
public interface AlgoritmoFloodFill {

    /**
     * Pinta a região conectada (4 vizinhos) com a mesma cor do pixel (x, y).
     * Usa servico.iniciar() no começo, servico.pintar() em cada pixel e servico.finalizar() no fim.
     * Cada vizinho guarda quem gerou ele, pra dar pra montar o caminho depois.
     *
     * @param destino Se não for null, para assim que pintar essa posição
     * @return A posição do destino (com os anteriores até o início), ou null se não chegou
     */
    Position executar(BufferedImage imagem, int x, int y, int novaCor,
                      Position destino, ImageService servico) throws IOException;
}
