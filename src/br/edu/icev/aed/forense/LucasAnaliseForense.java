package br.edu.icev.aed.forense;

import java.io.*;
import java.util.*;

public class LucasAnaliseForense implements AnaliseForenseAvancada {

    public LucasAnaliseForense(){}

    @Override
    public Set<String> encontrarSessoesInvalidas(String arquivo) throws IOException {
        Set<String> invalidas = new HashSet<>();
        Map<String, Deque<String>> pilhas = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo), 1_048_576)) {
            String linha = br.readLine();
            if (linha == null) return invalidas;
            while ((linha = br.readLine()) != null) {
                int p1 = linha.indexOf(',');
                int p2 = linha.indexOf(',', p1 + 1);
                int p3 = linha.indexOf(',', p2 + 1);
                int p4 = linha.indexOf(',', p3 + 1);
                if (p1 < 0 || p2 < 0 || p3 < 0 || p4 < 0) continue;
                String sessionId = linha.substring(p2 + 1, p3);
                String actionType = linha.substring(p3 + 1, p4);
                if (sessionId.isEmpty()) continue;
                pilhas.putIfAbsent(sessionId, new ArrayDeque<>());
                Deque<String> pilha = pilhas.get(sessionId);
                if ("LOGIN".equals(actionType)) {
                    if (!pilha.isEmpty()) invalidas.add(sessionId);
                    pilha.push("LOGIN");
                } else if ("LOGOUT".equals(actionType)) {
                    if (pilha.isEmpty()) invalidas.add(sessionId);
                    else pilha.pop();
                }
            }
        }

        for (Map.Entry<String, Deque<String>> e : pilhas.entrySet()) {
            if (!e.getValue().isEmpty()) invalidas.add(e.getKey());
        }

        return invalidas;
    }

    @Override
    public List<String> reconstruirLinhaTempo(String arquivo, String sessionId) throws IOException {
        if (sessionId == null || sessionId.isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(arquivo), 1_048_576)) {
            String linha = br.readLine();
            if (linha == null) return Collections.emptyList();
            while ((linha = br.readLine()) != null) {
                int p1 = linha.indexOf(',');
                int p2 = linha.indexOf(',', p1 + 1);
                int p3 = linha.indexOf(',', p2 + 1);
                int p4 = linha.indexOf(',', p3 + 1);
                if (p1 < 0 || p2 < 0 || p3 < 0 || p4 < 0) continue;
                String sessao = linha.substring(p2 + 1, p3);
                if (!sessao.equals(sessionId)) continue;
                String timestamp = linha.substring(0, p1);
                String actionType = linha.substring(p3 + 1, p4);
                String targetResource;
                int p5 = linha.indexOf(',', p4 + 1);
                if (p5 > 0) targetResource = linha.substring(p4 + 1, p5);
                else targetResource = linha.substring(p4 + 1);
                String evento = timestamp + ": " + actionType + " -> " + targetResource;
                result.add(evento);
            }
        }
        return result.isEmpty() ? Collections.emptyList() : result;
    }

    @Override
    public List<Alerta> priorizarAlertas(String arquivo, int n) throws IOException {
        if (n <= 0) return Collections.emptyList();
        PriorityQueue<Alerta> fila = new PriorityQueue<>(Comparator.comparingInt(Alerta::getSeverityLevel));
        try (BufferedReader br = new BufferedReader(new FileReader(arquivo), 1_048_576)) {
            String linha = br.readLine();
            if (linha == null) return Collections.emptyList();
            while ((linha = br.readLine()) != null) {
                String[] campos = linha.split(",", 7);
                if (campos.length < 7) continue;
                try {
                    long timestamp = Long.parseLong(campos[0]);
                    String userId = campos[1];
                    String sessionId = campos[2];
                    String actionType = campos[3];
                    String targetResource = campos[4];
                    int severityLevel = Integer.parseInt(campos[5]);
                    long bytesTransferred = campos[6].isEmpty() ? 0L : Long.parseLong(campos[6]);
                    Alerta a = new Alerta(timestamp, userId, sessionId, actionType, targetResource, severityLevel, bytesTransferred);
                    if (fila.size() < n) fila.add(a);
                    else if (a.getSeverityLevel() > fila.peek().getSeverityLevel()) {
                        fila.poll();
                        fila.add(a);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        List<Alerta> resultado = new ArrayList<>(fila);
        resultado.sort(Comparator.comparingInt(Alerta::getSeverityLevel).reversed());
        return resultado;
    }

    @Override
    public Map<Long, Long> encontrarPicosTransferencia(String caminhoArquivo) throws IOException {
        File f = new File(caminhoArquivo);
        if (!f.exists() || !f.isFile()) return Collections.emptyMap();
        Map<Long, Long> resultado = new HashMap<>();
        Deque<EventoTransferencia> pilha = new ArrayDeque<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f), 1_048_576)) {
            String linha = br.readLine();
            if (linha == null) return Collections.emptyMap();
            while ((linha = br.readLine()) != null) {
                String[] c = linha.split(",", 7);
                if (c.length < 7) continue;
                try {
                    long timestamp = Long.parseLong(c[0]);
                    long bytes = c[6].isEmpty() ? 0L : Long.parseLong(c[6]);
                    if (bytes <= 0) continue;
                    EventoTransferencia atual = new EventoTransferencia(timestamp, bytes);
                    while (!pilha.isEmpty() && pilha.peek().bytes < atual.bytes) {
                        EventoTransferencia menor = pilha.pop();
                        resultado.put(menor.timestamp, atual.timestamp);
                    }
                    pilha.push(atual);
                } catch (NumberFormatException ignored) {}
            }
        }
        return resultado.isEmpty() ? Collections.emptyMap() : resultado;
    }

    private static class EventoTransferencia {
        long timestamp;
        long bytes;
        EventoTransferencia(long t,long b){timestamp=t;bytes=b;}
    }

    @Override
    public Optional<List<String>> rastrearContaminacao(String caminhoArquivo, String recursoInicial, String recursoAlvo) throws IOException {
        File f = new File(caminhoArquivo);
        if (!f.exists() || !f.isFile()) return Optional.empty();
        Map<String, Set<String>> grafo = new HashMap<>();
        Map<String, String> ultimoRecursoPorSessao = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f), 1_048_576)) {
            String linha = br.readLine();
            if (linha == null) return Optional.empty();
            while ((linha = br.readLine()) != null) {
                int p1 = linha.indexOf(',');
                int p2 = linha.indexOf(',', p1 + 1);
                int p3 = linha.indexOf(',', p2 + 1);
                int p4 = linha.indexOf(',', p3 + 1);
                int p5 = linha.indexOf(',', p4 + 1);
                if (p1 < 0 || p2 < 0 || p3 < 0 || p4 < 0 || p5 < 0) continue;
                String sessionId = linha.substring(p2 + 1, p3);
                String recurso = linha.substring(p4 + 1, p5);
                if (ultimoRecursoPorSessao.containsKey(sessionId)) {
                    String anterior = ultimoRecursoPorSessao.get(sessionId);
                    grafo.computeIfAbsent(anterior, k -> new HashSet<>()).add(recurso);
                }
                ultimoRecursoPorSessao.put(sessionId, recurso);
            }
        }
        if (recursoInicial == null || recursoAlvo == null) return Optional.empty();
        Queue<List<String>> fila = new ArrayDeque<>();
        Set<String> visitados = new HashSet<>();
        fila.add(Collections.singletonList(recursoInicial));
        visitados.add(recursoInicial);
        while (!fila.isEmpty()) {
            List<String> caminho = fila.poll();
            String atual = caminho.get(caminho.size() - 1);
            if (atual.equals(recursoAlvo)) return Optional.of(caminho);
            for (String proximo : grafo.getOrDefault(atual, Collections.emptySet())) {
                if (!visitados.contains(proximo)) {
                    visitados.add(proximo);
                    List<String> novo = new ArrayList<>(caminho);
                    novo.add(proximo);
                    fila.add(novo);
                }
            }
        }
        return Optional.empty();
    }
}
