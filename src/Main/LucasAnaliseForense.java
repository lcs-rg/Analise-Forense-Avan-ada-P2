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
        Map<String, Integer> estado = new HashMap<>();
        Set<String> invalidas = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo), 8192)) {
            String linha;

            while ((linha = br.readLine()) != null) {

                int p1 = linha.indexOf(',');
                int p2 = linha.indexOf(',', p1 + 1);

                if (p1 == -1 || p2 == -1) {
                    continue;
                }

                String acao = linha.substring(0, p1).trim();
                String sessao = linha.substring(p1 + 1, p2).trim();

                int nivel = estado.getOrDefault(sessao, 0);

                if (acao.equals("LOGIN")) {
                    if (nivel > 0) {
                        invalidas.add(sessao);
                    }
                    estado.put(sessao, nivel + 1);
                }
                else if (acao.equals("LOGOUT")) {
                    if (nivel == 0) {
                        invalidas.add(sessao);
                    } else {
                        estado.put(sessao, nivel - 1);
                    }
                }
            }
        }

        for (Map.Entry<String, Integer> entry : estado.entrySet()) {
            if (entry.getValue() > 0) {
                invalidas.add(entry.getKey());
            }
        }

        return invalidas;
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
                        timeline.add(campos[3]);
                    }
                }
                //desenfileirando deque e enfileirando no arrayList final
                while (!timeline.isEmpty()) {
                    result.add(timeline.poll());
                }
            }
                        return result.isEmpty() ? Collections.emptyList() : result;

    }

    @Override
    public List<Alerta> priorizarAlertas(String arquivo, int n) throws IOException {
        if (n == 0){
            return Collections.emptyList();
        }
        // Implementar usando PriorityQueue<Alerta>
        PriorityQueue<Alerta> alertas = new PriorityQueue<>(
                Comparator.comparingInt(Alerta::getSeverityLevel)
        );
        List<Alerta> resultados = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(arquivo), 1024 * 1024)){
            br.readLine();
            String linha;
            while ((linha = br.readLine()) != null){
                String[] campos = linha.split(",");
                Alerta alerta = new Alerta(
                        Long.parseLong(campos[0]),
                        campos[1],
                        campos[2],
                        campos[3],
                        campos[4],
                        Integer.parseInt(campos[5]),
                        Long.parseLong(campos[6])
            if (alertas.size() < n){
                alertas.add(alerta);
            }
            }

            for (int i = 0; i < n; i++){
                resultados.add(alertas.poll());
            }
        }

        return resultados;

    }

    @Override
    public Map<Long, Long> encontrarPicosTransferencia(String arquivo) throws IOException {
        Map<Integer, Integer> picos = new HashMap<>();
        Stack<Map> eventos = new Stack<>();
        try (BufferedReader br = new BufferedReader(new FileReader(arquivo), 1024 * 1024)){
            br.readLine();
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] campos = linha.split(",");

            }
        }
        // Implementar usando Stack (Next Greater Element)
    }

    @Override
    public Optional<List<String>> rastrearContaminacao(String arquivo, String origem, String destino) throws IOException {
        // Implementar usando BFS em grafo
    }
}