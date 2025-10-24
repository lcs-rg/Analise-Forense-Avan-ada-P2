package Main;

import br.edu.icev.aed.forense.Alerta;
import br.edu.icev.aed.forense.AnaliseForenseAvancada;

import java.io.*;
import java.util.*;

public class LucasAnaliseForense implements AnaliseForenseAvancada {

    public LucasAnaliseForense(){

    }

    @Override
    public Set<String> encontrarSessoesInvalidas(String arquivo) throws IOException {
        LinkedHashMap<String, Stack>
        // Implementar usando Map<String, Stack<String>>
    }

    @Override
    public List<String> reconstruirLinhaTempo(String arquivo, String sessionId) throws IOException {
        // Implementar usando Queue<String>
        Queue<String> timeline = new ArrayDeque<>();//Deque para enfileirar histórico criada
        List<String> result = new ArrayList<>();//ArrayList do histórico final
            try (BufferedReader br = new BufferedReader(new FileReader(arquivo), 1024 * 1024)){//buffer com leitura de 1MB
                br.readLine();//lê cabeçalho
                String linha;
                while ((linha = br.readLine()) != null) {//loop para ler todas as linhas
                    String[] campos = linha.split(",");//separa os campos do log
                    if (campos[2].equals(sessionId)){//caso sessionId seja igual, adiciona a fila
                        timeline.add(campos[4]);
                    }
                }
                //desenfileirando da deque e enfileirando no arrayList final
                while (!timeline.isEmpty()) {
                    result.add(timeline.poll());
                }
            }
                        return result.isEmpty() ? Collections.emptyList() : result;

    }

    @Override
    public List<Alerta> priorizarAlertas(String arquivo, int n) throws IOException {
        // Implementar usando PriorityQueue<Alerta>
        List<String> alertas = new PriorityQueue<>();
    }

    @Override
    public Map<Long, Long> encontrarPicosTransferencia(String arquivo) throws IOException {
        // Implementar usando Stack (Next Greater Element)
    }

    @Override
    public Optional<List<String>> rastrearContaminacao(String arquivo, String origem, String destino) throws IOException {
        // Implementar usando BFS em grafo
    }
}