package br.edu.icev.aed.forense;

import java.io.*;
import java.util.*;

public class LucasAnaliseForense implements AnaliseForenseAvancada {

    public LucasAnaliseForense() {}

    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        if (line == null) return fields;
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        char prev = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"' && prev != '\\') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
            prev = c;
        }
        fields.add(sb.toString());
        for (int i = 0; i < fields.size(); i++) {
            String f = fields.get(i).trim();
            if (f.length() >= 2 && f.charAt(0) == '"' && f.charAt(f.length() - 1) == '"') {
                f = f.substring(1, f.length() - 1);
            }
            fields.set(i, f);
        }
        return fields;
    }

    @Override
    public Set<String> encontrarSessoesInvalidas(String arquivo) throws IOException {
        Set<String> invalidas = new HashSet<>();
        Map<String, Deque<String>> pilhas = new HashMap<>();
        File f = new File(arquivo);
        if (!f.exists() || !f.isFile()) return invalidas;
        try (BufferedReader br = new BufferedReader(new FileReader(f), 1_048_576)) {
            String linha = br.readLine();
            if (linha == null) return invalidas;
            while ((linha = br.readLine()) != null) {
                if (linha.trim().isEmpty()) continue;
                List<String> cols = parseCsvLine(linha);
                if (cols.size() < 4) continue;
                String sessionId = cols.get(2);
                String actionType = cols.get(3);
                if (sessionId == null || sessionId.isEmpty()) continue;
                pilhas.putIfAbsent(sessionId, new ArrayDeque<>());
                Deque<String> pilha = pilhas.get(sessionId);
                if ("LOGIN".equalsIgnoreCase(actionType)) {
                    if (!pilha.isEmpty()) invalidas.add(sessionId);
                    pilha.push("LOGIN");
                } else if ("LOGOUT".equalsIgnoreCase(actionType)) {
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
        File f = new File(arquivo);
        if (!f.exists() || !f.isFile()) return Collections.emptyList();
        try (BufferedReader br = new BufferedReader(new FileReader(f), 1_048_576)) {
            String linha = br.readLine();
            if (linha == null) return Collections.emptyList();
            while ((linha = br.readLine()) != null) {
                if (linha.trim().isEmpty()) continue;
                List<String> cols = parseCsvLine(linha);
                if (cols.size() < 5) continue;
                String sessao = cols.get(2);
                if (!sessionId.equals(sessao)) continue;
                String timestamp = cols.get(0);
                String actionType = cols.get(3);
                String targetResource = cols.size() > 4 ? cols.get(4) : "";
                String evento = timestamp + ": " + actionType + " -> " + targetResource;
                result.add(evento);
            }
        }
        return result.isEmpty() ? Collections.emptyList() : result;
    }

    @Override
    public List<Alerta> priorizarAlertas(String arquivo, int n) throws IOException {
        if (n <= 0) return Collections.emptyList();
        File f = new File(arquivo);
        if (!f.exists() || !f.isFile()) return Collections.emptyList();
        PriorityQueue<Alerta> fila = new PriorityQueue<>(Comparator.comparingInt(Alerta::getSeverityLevel));
        try (BufferedReader br = new BufferedReader(new FileReader(f), 1_048_576)) {
            String linha = br.readLine();
            if (linha == null) return Collections.emptyList();
            while ((linha = br.readLine()) != null) {
                if (linha.trim().isEmpty()) continue;
                List<String> cols = parseCsvLine(linha);
                if (cols.size() < 7) continue;
                try {
                    long timestamp = Long.parseLong(cols.get(0).trim());
                    String userId = cols.get(1);
                    String sessionId = cols.get(2);
                    String actionType = cols.get(3);
                    String targetResource = cols.get(4);
                    int severityLevel = Integer.parseInt(cols.get(5).trim());
                    long bytesTransferred = 0L;
                    String bytesStr = cols.get(6).trim();
                    if (!bytesStr.isEmpty()) {
                        try { bytesTransferred = Long.parseLong(bytesStr); } catch (NumberFormatException ex) { bytesTransferred = 0L; }
                    }
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
                if (linha.trim().isEmpty()) continue;
                List<String> cols = parseCsvLine(linha);
                if (cols.size() < 7) continue;
                String action = cols.get(3);
                if (!"DATA_TRANSFER".equalsIgnoreCase(action)) {
                    String bytesStr = cols.get(6).trim();
                    if (bytesStr.isEmpty()) continue;
                }
                try {
                    long timestamp = Long.parseLong(cols.get(0).trim());
                    String bytesStr = cols.get(6).trim();
                    long bytes = 0L;
                    if (!bytesStr.isEmpty()) {
                        try { bytes = Long.parseLong(bytesStr); } catch (NumberFormatException ex) { bytes = 0L; }
                    }
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
        EventoTransferencia(long t, long b) { timestamp = t; bytes = b; }
    }

    @Override
    public Optional<List<String>> rastrearContaminacao(String caminhoArquivo, String recursoInicial, String recursoAlvo) throws IOException {
        if (recursoInicial == null || recursoAlvo == null) return Optional.empty();
        File f = new File(caminhoArquivo);
        if (!f.exists() || !f.isFile()) return Optional.empty();
        Map<String, Set<String>> grafo = new HashMap<>();
        Map<String, String> ultimoRecursoPorSessao = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f), 1_048_576)) {
            String linha = br.readLine();
            if (linha == null) return Optional.empty();
            while ((linha = br.readLine()) != null) {
                if (linha.trim().isEmpty()) continue;
                List<String> cols = parseCsvLine(linha);
                if (cols.size() < 5) continue;
                String sessionId = cols.get(2);
                String recurso = cols.get(4);
                if (sessionId == null || sessionId.isEmpty() || recurso == null) continue;
                if (ultimoRecursoPorSessao.containsKey(sessionId)) {
                    String anterior = ultimoRecursoPorSessao.get(sessionId);
                    grafo.computeIfAbsent(anterior, k -> new HashSet<>()).add(recurso);
                }
                ultimoRecursoPorSessao.put(sessionId, recurso);
            }
        }
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
