package comum;

/**
 * Posição na imagem (linha = y, coluna = x).
 * Também guarda a posição anterior, então dá pra seguir o caminho de volta até o início.
 */
public class Position {
    public final int row;
    public final int col;
    public final Position anterior;
    /** Distância até o início (0 no pixel inicial), usada no ritmo da animação. */
    public final int distancia;

    public Position(int row, int col) {
        this(row, col, null);
    }

    public Position(int row, int col, Position anterior) {
        this.row = row;
        this.col = col;
        this.anterior = anterior;
        this.distancia = anterior == null ? 0 : anterior.distancia + 1;
    }

    public boolean mesmaPosicao(Position outra) {
        return outra != null && row == outra.row && col == outra.col;
    }
}
