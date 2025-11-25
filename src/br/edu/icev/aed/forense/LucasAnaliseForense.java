package br.edu.icev.aed.forense;

import java.io.*;
import java.util.*;

public class LucasAnaliseForense implements AnaliseForenseAvancada {

    public LucasAnaliseForense(){

    }

    @Override
    public Set<String> encontrarSessoesInvalidas(String arquivo) throws IOException {
        Set<String> invalidas = new HashSet<>();
        Map<String, Stack<String>> pilhas = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha = br.readLine(); // Pula cabeçalho
            if (linha == null) return invalidas;

            while ((linha = br.readLine()) != null) {
                String[] campos = linha.split(",");
                if (campos.length < 4) continue;

                // TIMESTAMP,USER_ID,SESSION_ID,ACTION_TYPE,...
                String sessionId = campos[2];
                String actionType = campos[3];

                if (sessionId.isEmpty()) continue;

                pilhas.putIfAbsent(sessionId, new Stack<>());
                Stack<String> pilha = pilhas.get(sessionId);

                if (actionType.equals("LOGIN")) {
                    // Se já tem LOGIN na pilha, é LOGIN aninhado - INVÁLIDO
                    if (!pilha.isEmpty()) {
                        invalidas.add(sessionId);
                    }
                    pilha.push("LOGIN");

                } else if (actionType.equals("LOGOUT")) {
                    // LOGOUT sem LOGIN correspondente - INVÁLIDO
                    if (pilha.isEmpty()) {
                        invalidas.add(sessionId);
                    } else {
                        pilha.pop();
                    }
                }
            }
        }

        // Sessões que terminaram sem LOGOUT - INVÁLIDO
        for (Map.Entry<String, Stack<String>> entry : pilhas.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                invalidas.add(entry.getKey());
            }
        }

        return invalidas;
    }

    @Override
    public List<String> reconstruirLinhaTempo(String arquivo, String sessionId) throws IOException {
        if (sessionId == null || sessionId.isEmpty()) return Collections.emptyList();

        Queue<String> fila = new ArrayDeque<>();

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha = br.readLine(); // Pula cabeçalho
            if (linha == null) return Collections.emptyList();

            while ((linha = br.readLine()) != null) {
                String[] campos = linha.split(",");
                if (campos.length < 5) continue;

                // TIMESTAMP,USER_ID,SESSION_ID,ACTION_TYPE,TARGET_RESOURCE,...
                String sessao = campos[2];

                if (!sessao.equals(sessionId)) continue;

                String timestamp = campos[0];
                String actionType = campos[3];
                String targetResource = campos[4];

                // Formato: "TIMESTAMP: ACTION_TYPE -> TARGET_RESOURCE"
                String evento = timestamp + ": " + actionType + " -> " + targetResource;
                fila.add(evento);
            }
        }

        return new ArrayList<>(fila);
    }

    @Override
    public List<Alerta> priorizarAlertas(String arquivo, int n) throws IOException {
        if (n <= 0) return Collections.emptyList();

        // Min-heap para manter os N alertas de maior severidade
        PriorityQueue<Alerta> fila = new PriorityQueue<>(Comparator.comparingInt(Alerta::getSeverityLevel));

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha = br.readLine(); // Pula cabeçalho
            if (linha == null) return Collections.emptyList();

            while ((linha = br.readLine()) != null) {
                String[] campos = linha.split(",");
                if (campos.length < 7) continue;

                try {
                    long timestamp = Long.parseLong(campos[0]);
                    String userId = campos[1];
                    String sessionId = campos[2];
                    String actionType = campos[3];
                    String targetResource = campos[4];
                    int severityLevel = Integer.parseInt(campos[5]);
                    long bytesTransferred = campos[6].isEmpty() ? 0 : Long.parseLong(campos[6]);

                    Alerta alerta = new Alerta(
                            timestamp, userId, sessionId, actionType,
                            targetResource, severityLevel, bytesTransferred
                    );

                    if (fila.size() < n) {
                        fila.add(alerta);
                    } else if (alerta.getSeverityLevel() > fila.peek().getSeverityLevel()) {
                        fila.poll();
                        fila.add(alerta);
                    }

                } catch (NumberFormatException ignored) {}
            }
        }

        // Converter para lista e ordenar do maior para o menor
        List<Alerta> resultado = new ArrayList<>(fila);
        resultado.sort(Comparator.comparingInt(Alerta::getSeverityLevel).reversed());
        return resultado;
    }

    @Override
    public Map<Long, Long> encontrarPicosTransferencia(String caminhoArquivo) throws IOException {
        File f = new File(caminhoArquivo);
        if (!f.exists() || !f.isFile()) return Collections.emptyMap();

        List<EventoTransferencia> eventos = new ArrayList<>();

        // Ler todos os eventos de transferência
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String linha = br.readLine(); // Pula cabeçalho
            if (linha == null) return Collections.emptyMap();

            while ((linha = br.readLine()) != null) {
                String[] campos = linha.split(",");
                if (campos.length < 7) continue;

                try {
                    long timestamp = Long.parseLong(campos[0]);
                    long bytes = campos[6].isEmpty() ? 0 : Long.parseLong(campos[6]);

                    if (bytes > 0) { // Só considera transferências com bytes > 0
                        eventos.add(new EventoTransferencia(timestamp, bytes));
                    }
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }

        // Usar Stack para encontrar Next Greater Element
        Map<Long, Long> resultado = new HashMap<>();
        Stack<EventoTransferencia> pilha = new Stack<>();

        for (EventoTransferencia atual : eventos) {
            // Enquanto o topo da pilha tem menos bytes que o atual
            while (!pilha.isEmpty() && pilha.peek().bytes < atual.bytes) {
                EventoTransferencia menor = pilha.pop();
                resultado.put(menor.timestamp, atual.timestamp);
            }
            pilha.push(atual);
        }

        // Eventos restantes na pilha não têm próximo maior
        // (podem ser adicionados com valor -1 ou simplesmente não incluídos)

        return resultado;
    }

    // Classe auxiliar para o Desafio 4
    private static class EventoTransferencia {
        long timestamp;
        long bytes;

        EventoTransferencia(long timestamp, long bytes) {
            this.timestamp = timestamp;
            this.bytes = bytes;
        }
    }

    @Override
    public Optional<List<String>> rastrearContaminacao(String caminhoArquivo, String recursoInicial, String recursoAlvo) throws IOException {
        File f = new File(caminhoArquivo);
        if (!f.exists() || !f.isFile()) return Optional.empty();

        // Construir grafo: recurso -> lista de recursos acessados depois na mesma sessão
        Map<String, Set<String>> grafo = new HashMap<>();
        Map<String, String> ultimoRecursoPorSessao = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String linha = br.readLine(); // Pula cabeçalho
            if (linha == null) return Optional.empty();

            while ((linha = br.readLine()) != null) {
                String[] campos = linha.split(",");
                if (campos.length < 5) continue;

                String sessionId = campos[2];
                String recurso = campos[4];

                // Se já havia um recurso acessado nesta sessão, criar aresta
                if (ultimoRecursoPorSessao.containsKey(sessionId)) {
                    String recursoAnterior = ultimoRecursoPorSessao.get(sessionId);
                    grafo.computeIfAbsent(recursoAnterior, k -> new HashSet<>()).add(recurso);
                }

                ultimoRecursoPorSessao.put(sessionId, recurso);
            }
        }

        // BFS para encontrar caminho mais curto
        if (!grafo.containsKey(recursoInicial)) return Optional.empty();

        Queue<List<String>> fila = new ArrayDeque<>();
        Set<String> visitados = new HashSet<>();

        fila.add(Collections.singletonList(recursoInicial));
        visitados.add(recursoInicial);

        while (!fila.isEmpty()) {
            List<String> caminho = fila.poll();
            String atual = caminho.get(caminho.size() - 1);

            if (atual.equals(recursoAlvo)) {
                return Optional.of(caminho);
            }

            for (String proximo : grafo.getOrDefault(atual, Collections.emptySet())) {
                if (!visitados.contains(proximo)) {
                    visitados.add(proximo);
                    List<String> novoCaminho = new ArrayList<>(caminho);
                    novoCaminho.add(proximo);
                    fila.add(novoCaminho);
                }
            }
        }

        return Optional.empty();
    }
}