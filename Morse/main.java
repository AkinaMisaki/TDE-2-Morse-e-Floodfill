class Node {
    String val_morse;
    char val_letra;
    Node esquerda;
    Node direita;

    public Node(String val_morse , char val_letra){
        this.val_morse = val_morse;
        this.val_letra = val_letra;
        esquerda = null;
        direita = null;
    }
}

class ArvoreBinaria{
    public Node root;

    public ArvoreBinaria(){
        this.root = new Node("", ' ');

        inserir(".-", 'A');
        inserir("-...", 'B');
        inserir("-.-.", 'C');
        inserir("-..", 'D');
        inserir(".", 'E');
        inserir("..-.", 'F');
        inserir("--.", 'G');
        inserir("....", 'H');
        inserir("..", 'I');
        inserir(".---", 'J');
        inserir("-.-", 'K');
        inserir(".-..", 'L');
        inserir("--", 'M');
        inserir("-.", 'N');
        inserir("---", 'O');
        inserir(".--.", 'P');
        inserir("--.-", 'Q');
        inserir(".-.", 'R');
        inserir("...", 'S');
        inserir("-", 'T');
        inserir("..-", 'U');
        inserir("...-", 'V');
        inserir(".--", 'W');
        inserir("-..-", 'X');
        inserir("-.--", 'Y');
        inserir("--..", 'Z');
    }

    public void inserir(String val_morse , char letra){
        Node atual = this.root;

        for (char i : val_morse.toCharArray()){

            if (i == '.'){
                if (atual.esquerda == null){
                    atual.esquerda = new Node("" , ' ');
                }
                atual = atual.esquerda;
            }

            else if (i == '-'){
                if (atual.direita == null){
                    atual.direita = new Node("" , ' ');
                }
                atual = atual.direita;


            } else {
                System.out.println("valor fora do escopo");
                break;
            }
        }

        atual.val_morse = val_morse;
        atual.val_letra = letra;
    }

    public void decodificar(String morse){
        String resultado = "";
        Node atual = this.root;


        for (char i : morse.toCharArray()){

            if (i == '.'){
                atual = atual.esquerda;
            }

            else if (i == '-') {
                atual = atual.direita;
            }

            else if (i == ' ') {
                if (atual != this.root) {
                    resultado += atual.val_letra;
                    atual = this.root;
                }

            }

            else if (i == '/') {
                if (atual != this.root) {
                    resultado += atual.val_letra;
                    atual = this.root;
                }
                resultado += ' ';
            }

            else {
                System.out.println("valor invalido inserido");
                break;
            }
        }
        if (atual != this.root) {
            resultado += atual.val_letra;
        }
        System.out.println(resultado);

    }
}

public class Main {
    public static void main(String[] args) {
        ArvoreBinaria Arvore = new ArvoreBinaria();
        Arvore.decodificar("...  / --- / ...");

    }
}
