Pasta da versão do Flood Fill com PILHA.

Crie aqui o FloodFillPilha.java, usando o fila/FloodFillFila.java como modelo:

    package pilha;

    import comum.AlgoritmoFloodFill;
    import comum.ImageService;
    import comum.Position;
    import java.awt.image.BufferedImage;
    import java.io.IOException;

    public class FloodFillPilha implements AlgoritmoFloodFill {

        // Node e Stack próprios (push e pop no topo).
        // NÃO pode usar java.util.Stack, Deque nem Queue.

        @Override
        public Position executar(BufferedImage imagem, int x, int y, int novaCor,
                                 Position destino, ImageService servico) throws IOException {
            // 1. Valida (x, y), guarda a corOriginal e, se novaCor == corOriginal, return null
            // 2. servico.iniciar(imagem); pilha.push(new Position(y, x));
            // 3. Enquanto a pilha não estiver vazia: p = pilha.pop();
            //      Fora da imagem ou cor != corOriginal -> continue
            //      Pinta o pixel com servico.pintar(imagem, p, novaCor);
            //      Se p.mesmaPosicao(destino) -> servico.finalizar(imagem); return p;
            //      Push dos 4 vizinhos: new Position(linha, coluna, p)
            // 4. servico.finalizar(imagem); return null;
        }
    }

------------------------------------------------------------------------------
COMO FAZER O BOTÃO "Balde (Pilha)" APARECER NO EDITOR
------------------------------------------------------------------------------

Pra fazer a pilha aparecer no editor, vc vai ter q editar uma método no arquivo main.java da pasta do floodfill

criarPilha()  (perto da linha 19)

Tá assim:

    private static AlgoritmoFloodFill criarPilha() {
        return null;
    }

Troca pra:

    private static AlgoritmoFloodFill criarPilha() {
        return new pilha.FloodFillPilha();
    }

Por que funciona: logo abaixo, no main(), o Main faz

    AlgoritmoFloodFill pilha = criarPilha();
    if (pilha != null) {
        editor.adicionarAlgoritmo("Pilha", pilha);   // <- Isso cria o botão
    }

Enquanto o criarPilha() devolver null, o botão não é criado. O "return null" tá lá
de propósito, porque com "new pilha.FloodFillPilha()" o projeto não compilaria
enquanto o arquivo da pilha não existisse.

O EditorGrid não sabe o que é pilha nem fila, ele só cria um botão "Balde (nome)"
pra cada algoritmo que chega pelo adicionarAlgoritmo(...). Por isso não precisa mudar nada nele.

Depois disso parece o botão "Balde (Pilha)" no editor, e a pilha também fica
disponível no "Labirinto infinito..." (com a opção "Alternar" entre pilha e fila).

Pra compilar e rodar, na pasta Floodfill:
    javac Main.java
    java Main
