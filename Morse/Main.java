import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

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
        morse = morse.replace("\n", " ").replace("\r", "");
        if (!morse.matches("^[\\.\\-\\/ ]+$")) {
            System.out.println("[ERRO] A entrada contém caracteres inválidos. Utilize apenas '.', '-', espaço ou '/'.");
            return; // Interrompe a execução
        }
        int espaco = 0;
        String resultado = "";
        Node atual = this.root;

        for (char i : morse.toCharArray()) {
            if (i == ' '){
                espaco = 0;
            }
            if (espaco != 1){
                if (i == '.') {
                    if (atual.esquerda != null) {
                        atual = atual.esquerda;
                    } else {
                        System.out.println("\n[ERRO] Sequência '" + i + "' levou a um caminho inexistente na árvore. Resetando busca.");
                        atual = this.root;
                        espaco = 1; 
                    }
                } else if (i == '-') {
                    if (atual.direita != null) {
                        atual = atual.direita;
                    } else {
                        System.out.println("\n[ERRO] Sequência '" + i + "' levou a um caminho inexistente na árvore. Resetando busca.");
                        atual = this.root;
                        espaco = 1;
                    }
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
                }
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
            System.err.println("[ERRO] Falha ao ler o arquivo: " + e.getMessage());
        }
    }

    public void exibirArvore() {
        System.out.println("\n////////////////////////////////////////");
        System.out.println("   REPRESENTAÇÃO VISUAL DA ÁRVORE MORSE");
        System.out.println("////////////////////////////////////////");
        System.out.println("* Os 'Traços' (-) sobem, os 'Pontos' (.) descem.");
        System.out.println("////////////////////////////////////////\n");
        imprimirRecursivo(this.root, "", false, true);
        System.out.println("\n////////////////////////////////////////\n");
    }
    private void imprimirRecursivo(Node no, String prefixo, boolean isEsquerda, boolean isRaiz) {
        if (no == null) {
            return;
        }
        if (!isRaiz) {
            imprimirRecursivo(no.direita, prefixo + (isEsquerda ? "│       " : "        "), false, false);
            System.out.println(prefixo + (isEsquerda ? "└── " : "┌── ") + no.val_letra + " (" + no.val_morse + ")");
            imprimirRecursivo(no.esquerda, prefixo + (isEsquerda ? "        " : "│       "), true, false);
        } else {
            imprimirRecursivo(no.direita, prefixo + "        ", false, false);
            System.out.println(prefixo + "[RAIZ]");
            imprimirRecursivo(no.esquerda, prefixo + "        ", true, false);
        }
    }
}

class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ArvoreBinaria arvore = new ArvoreBinaria();
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
                    System.out.print("Entrada: ");
                    String valor_morse = scanner.nextLine();
                    System.out.println("\nProcessando...");
                    arvore.decodificar(valor_morse);
                    System.out.println("////////////////////////////////////////");
                    break;

                case 2:
                    System.out.println("\nAbrindo janela de seleção de arquivo...");
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Selecione o arquivo de texto com o Código Morse");
                    FileNameExtensionFilter filter = new FileNameExtensionFilter("Arquivos de Texto (*.txt)", "txt");
                    fileChooser.setFileFilter(filter);
                    int retorno = fileChooser.showOpenDialog(null);
                    if (retorno == JFileChooser.APPROVE_OPTION) {
                        String caminho = fileChooser.getSelectedFile().getAbsolutePath();
                        System.out.println("Arquivo selecionado: " + caminho);
                        arvore.decodificarArquivo(caminho);
                    } else {
                        System.out.println("Seleção de arquivo cancelada pelo usuário.");
                    }
                    System.out.println("////////////////////////////////////////");
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
    }
}