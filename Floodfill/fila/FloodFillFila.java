package fila;

import comum.AlgoritmoFloodFill;
import comum.ImageService;
import comum.Position;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Flood Fill com uma fila feita na mão (sem java.util.Queue nem Deque).
 * Pinta em camadas, tipo busca em largura, então o caminho até o destino é sempre o mais curto.
 */
public class FloodFillFila implements AlgoritmoFloodFill {

    /** Nó da lista encadeada da fila. */
    static class Node {
        Position value;
        Node next;

        Node(Position value) {
            this.value = value;
        }
    }

    /** Fila encadeada: entra pelo fim (tail) e sai pelo começo (head). */
    static class Queue {
        private Node head;
        private Node tail;

        void enqueue(Position p) {
            Node node = new Node(p);
            if (tail == null) {
                head = node;
            } else {
                tail.next = node;
            }
            tail = node;
        }

        Position dequeue() {
            if (head == null) {
                throw new IllegalStateException("Fila vazia");
            }
            Position p = head.value;
            head = head.next;
            if (head == null) {
                tail = null;
            }
            return p;
        }

        boolean isEmpty() {
            return head == null;
        }
    }

    @Override
    public Position executar(BufferedImage imagem, int x, int y, int novaCor, int limiar,
                             Position destino, ImageService servico) throws IOException {
        int largura = imagem.getWidth();
        int altura = imagem.getHeight();

        if (x < 0 || x >= largura || y < 0 || y >= altura) {
            throw new IllegalArgumentException("Coordenada (" + x + ", " + y
                    + ") fora da imagem " + largura + "x" + altura);
        }

        int corOriginal = rgb(imagem.getRGB(x, y));
        novaCor = rgb(novaCor);

        // Se a cor nova é igual à original, não tem o que fazer.
        if (corOriginal == novaCor) {
            return null;
        }

        servico.iniciar(imagem);

        Queue fila = new Queue();
        fila.enqueue(new Position(y, x));

        while (!fila.isEmpty()) {
            Position p = fila.dequeue();

            // Fora da imagem, pula.
            if (p.row < 0 || p.row >= altura || p.col < 0 || p.col >= largura) {
                continue;
            }
            // Já está na cor nova (já pintado) ou a cor está longe demais da original (parede), pula também.
            int cor = rgb(imagem.getRGB(p.col, p.row));
            if (cor == novaCor || !AlgoritmoFloodFill.corProxima(cor, corOriginal, limiar)) {
                continue;
            }

            // Pintar já marca o pixel como visitado (o serviço cuida da animação e dos BMPs).
            servico.pintar(imagem, p, novaCor);

            // Chegou no destino. Seguindo o p.anterior dá pra voltar até o início.
            if (p.mesmaPosicao(destino)) {
                servico.finalizar(imagem);
                return p;
            }

            fila.enqueue(new Position(p.row - 1, p.col, p)); // Cima
            fila.enqueue(new Position(p.row + 1, p.col, p)); // Baixo
            fila.enqueue(new Position(p.row, p.col - 1, p)); // Esquerda
            fila.enqueue(new Position(p.row, p.col + 1, p)); // Direita
        }

        servico.finalizar(imagem);
        return null;
    }

    /** Tira o canal alfa pra comparar só o RGB. */
    private static int rgb(int argb) {
        return argb & 0xFFFFFF;
    }
}
