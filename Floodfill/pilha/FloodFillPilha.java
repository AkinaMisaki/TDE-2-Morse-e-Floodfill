package pilha;

import comum.AlgoritmoFloodFill;
import comum.ImageService;
import comum.Position;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class FloodFillPilha implements AlgoritmoFloodFill {

    // Nó
    static class Node {
        Position value;
        Node next;

        Node(Position value) {
            this.value = value;
        }
    }


    static class Stack {
        private Node top;

        void push(Position p) {
            Node node = new Node(p);
            node.next = top; 
            top = node;
        }

        Position pop() {
            if (top == null) {
                throw new IllegalStateException("Pilha vazia");
            }
            Position p = top.value;
            top = top.next;
            return p;
        }

        boolean isEmpty() {
            return top == null;
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

        // Se a cor nova é igual à original
        if (corOriginal == novaCor) {
            return null;
        }

        servico.iniciar(imagem);

        Stack pilha = new Stack();
        pilha.push(new Position(y, x));

        while (!pilha.isEmpty()) {
            Position p = pilha.pop();
            if (p.row < 0 || p.row >= altura || p.col < 0 || p.col >= largura) {
                continue;
            }

            // Já está na cor nova ou a cor está longe demais da original
            int cor = rgb(imagem.getRGB(p.col, p.row));
            if (cor == novaCor || !AlgoritmoFloodFill.corProxima(cor, corOriginal, limiar)) {
                continue;
            }
            servico.pintar(imagem, p, novaCor);


            if (p.mesmaPosicao(destino)) {
                servico.finalizar(imagem);
                return p;
            }

            pilha.push(new Position(p.row - 1, p.col, p)); // Cima
            pilha.push(new Position(p.row + 1, p.col, p)); // Baixo
            pilha.push(new Position(p.row, p.col - 1, p)); // Esquerda
            pilha.push(new Position(p.row, p.col + 1, p)); // Direita
        }

        servico.finalizar(imagem);
        return null;
    }

    private static int rgb(int argb) {
        return argb & 0xFFFFFF;
    }
}