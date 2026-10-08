package comum;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Interface que as versões com pilha e fila seguem, assim o EditorGrid aceita qualquer uma.
 */
public interface AlgoritmoFloodFill {

    /**
     * Pinta a região conectada (4 vizinhos) com cor próxima à do pixel (x, y).
     * Usa servico.iniciar() no começo, servico.pintar() em cada pixel e servico.finalizar() no fim.
     * Cada vizinho guarda quem gerou ele, pra dar pra montar o caminho depois.
     *
     * @param limiar  Quanto a cor pode diferir da cor do pixel (x, y), de 0 a 255 (0 = só a cor exata)
     * @param destino Se não for null, para assim que pintar essa posição
     * @return A posição do destino (com os anteriores até o início), ou null se não chegou
     */
    Position executar(BufferedImage imagem, int x, int y, int novaCor, int limiar,
                      Position destino, ImageService servico) throws IOException;

    /** Diz se duas cores RGB são próximas: nenhum canal (R, G, B) pode diferir mais que o limiar. */
    static boolean corProxima(int a, int b, int limiar) {
        int r = Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF));
        int g = Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF));
        int azul = Math.abs((a & 0xFF) - (b & 0xFF));
        return r <= limiar && g <= limiar && azul <= limiar;
    }
}
