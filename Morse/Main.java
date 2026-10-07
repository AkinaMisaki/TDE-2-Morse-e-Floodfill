import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;


class Node {
    String val_morse;
    char val_letra;
    Node esquerda;
    Node direita;
    public Node(String val_morse, char val_letra) {
        this.val_morse = val_morse;
        this.val_letra = val_letra;
        esquerda = null;
        direita = null;
    }
}
class ArvoreBinaria {
    public Node root;
    public ArvoreBinaria() {
        this.root = new Node("", ' ');
        inserir(".-", 'A');   inserir("-...", 'B'); inserir("-.-.", 'C');
        inserir("-..", 'D');  inserir(".", 'E');    inserir("..-.", 'F');
        inserir("--.", 'G');  inserir("....", 'H'); inserir("..", 'I');
        inserir(".---", 'J'); inserir("-.-", 'K');  inserir(".-..", 'L');
        inserir("--", 'M');   inserir("-.", 'N');   inserir("---", 'O');
        inserir(".--.", 'P'); inserir("--.-", 'Q'); inserir(".-.", 'R');
        inserir("...", 'S');  inserir("-", 'T');    inserir("..-", 'U');
        inserir("...-", 'V'); inserir(".--", 'W');  inserir("-..-", 'X');
        inserir("-.--", 'Y'); inserir("--..", 'Z');
        inserir(".----", '1'); inserir("..---", '2'); inserir("...--", '3');
        inserir("....-", '4'); inserir(".....", '5'); inserir("-....", '6');
        inserir("--...", '7'); inserir("---..", '8'); inserir("----.", '9');
        inserir("-----", '0');
    }
    public void inserir(String val_morse, char letra) {
        Node atual = this.root;
        for (char i : val_morse.toCharArray()) {
            if (i == '.') {
                if (atual.esquerda == null) {
                    atual.esquerda = new Node("", ' ');
                }
                atual = atual.esquerda;
            } else if (i == '-') {
                if (atual.direita == null) {
                    atual.direita = new Node("", ' ');
                }
                atual = atual.direita;
            } else {
                System.out.println("Erro na árvore: caractere inválido ao inserir.");
                break;
            }
        }
        atual.val_morse = val_morse;
        atual.val_letra = letra;
    }
    public void decodificar(String morse) {
        String resultado = "";
        Node atual = this.root;
        for (char i : morse.toCharArray()) {
            if (i == '.') {
                if (atual.esquerda != null) atual = atual.esquerda;
            } else if (i == '-') {
                if (atual.direita != null) atual = atual.direita;
            } else if (i == ' ') {
                if (atual != this.root) {
                    resultado += atual.val_letra;
                    atual = this.root;
                }
            } else if (i == '/') {
                if (atual != this.root) {
                    resultado += atual.val_letra;
                    atual = this.root;
                }
                resultado += ' ';
            } else {
                System.out.println("\n[Aviso] Caractere ignorado na entrada: " + i);
            }
        }
        if (atual != this.root) {
            resultado += atual.val_letra;
        }
        System.out.println("Mensagem decodificada: " + resultado);
    }

    public void decodificarArquivo(String caminhoArquivo) {
        try {

            String codigoMorse = Files.readString(Path.of(caminhoArquivo)).trim();
            System.out.println("Conteúdo lido do arquivo:\n" + codigoMorse);
            decodificar(codigoMorse);

        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo: " + e.getMessage());
        }
    }

    public void exibirArvore() {
        System.out.println("\n////////////////////////////////////////");
        System.out.println("   REPRESENTAÇÃO VISUAL DA ÁRVORE MORSE");
        System.out.println("////////////////////////////////////////");
        System.out.println("* Os 'Traços' (-) sobem, os 'Pontos' (.) descem.");
        System.out.println("////////////////////////////////////////\n");
        imprimirRecursivo(this.root, 0);
        System.out.println("\n////////////////////////////////////////\n");
    }
    private void imprimirRecursivo(Node no, int nivel) {
        if (no == null) {
            return;
        }
        imprimirRecursivo(no.direita, nivel + 1);
        for (int i = 0; i < nivel; i++) {
            System.out.print("        ");
        }
        if (no == this.root) {
            System.out.println("[RAIZ]");
        } else {
            System.out.println(no.val_letra + " (" + no.val_morse + ")");
        }

        imprimirRecursivo(no.esquerda, nivel + 1);
    }
}
class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ArvoreBinaria arvore = new ArvoreBinaria();

        String caminho_arquivo = "arquivo.txt";
        int opcao = -1;
        System.out.println("////////////////////////////////////////");
        System.out.println("    BEM-VINDO AO DECODIFICADOR MORSE    ");
        System.out.println("////////////////////////////////////////");
        while (opcao != 0) {
            System.out.println("\nEscolha uma opção:");
            System.out.println("1 - Decodificar código Morse");
            System.out.println("2 - Decodificar morse em txt");
            System.out.println("3 - Visualizar Árvore Binária");
            System.out.println("0 - Sair");
            System.out.print("Opção: ");
            try {
                opcao = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                opcao = -1;
            }
            switch (opcao) {
                case 1:
                    System.out.println("\nDigite o código morse (use espaço entre letras e '/' entre palavras):");
                    System.out.println("Exemplo: .... --- .-.. .- / -- ..- -. -.. ---");
                    System.out.print("Entrada: ");
                    String valor_morse = scanner.nextLine();
                    System.out.println("\nProcessando...");
                    arvore.decodificar(valor_morse);
                    System.out.println("////////////////////////////////////////");
                    break;

                case 2:
                    arvore.decodificarArquivo(caminho_arquivo);
                    break;
                case 3:
                    arvore.exibirArvore();
                    break;
                case 0:
                    System.out.println("Encerrando o programa... Até logo!");
                    break;

                default:
                    System.out.println("Opção inválida! Tente novamente.");
                    break;
            }
        }
        scanner.close();
        arvore.decodificarArquivo(caminho_arquivo);
    }

}
