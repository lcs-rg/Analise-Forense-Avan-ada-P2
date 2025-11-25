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
        Set<String> invalidas = new HashSet<>();
        Map<String, Integer> estado = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo), 1_048_576)) {
            String linha = br.readLine();
            if (linha == null) return invalidas;
            while ((linha = br.readLine()) != null) {
                int p1 = linha.indexOf(',');
                int p2 = linha.indexOf(',', p1 + 1);
                if (p1 < 0 || p2 < 0) continue;

                String acao = linha.substring(0, p1);
                String sessao = linha.substring(p1 + 1, p2);
                if (sessao.isEmpty()) continue;

                int valor = estado.getOrDefault(sessao, 0);

                if (acao.equals("LOGIN")) {
                    if (valor > 0) invalidas.add(sessao);
                    estado.put(sessao, valor + 1);
                } else if (acao.equals("LOGOUT")) {
                    if (valor == 0) invalidas.add(sessao);
                    else estado.put(sessao, valor - 1);
                }
            }
        }

        for (var e : estado.entrySet()) {
            if (e.getValue() > 0) invalidas.add(e.getKey());
        }

        return invalidas;
    }

    @Override
    public List<String> reconstruirLinhaTempo(String arquivo, String sessionId) throws IOException {

        List<String> result = new ArrayList<>();
        String linha;

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo), 1_048_576)) {

            br.readLine();

            while ((linha = br.readLine()) != null) {

                int p1 = linha.indexOf(',');
                int p2 = linha.indexOf(',', p1 + 1);
                int p3 = linha.indexOf(',', p2 + 1);

                if (p1 < 0 || p2 < 0 || p3 < 0) continue;

                String sessao = linha.substring(p2 + 1, p3);

                if (sessao.equals(sessionId)) {
                    result.add(linha.substring(p3 + 1));
                }
            }
        }

        return result;
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