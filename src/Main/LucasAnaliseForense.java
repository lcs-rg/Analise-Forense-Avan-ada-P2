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
        if (sessionId == null || sessionId.isEmpty()) return Collections.emptyList();

        Queue<String> fila = new ArrayDeque<>();
        List<String> result = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo), 1_048_576)) {
            String linha = br.readLine();
            if (linha == null) return Collections.emptyList();

            while ((linha = br.readLine()) != null) {
                int p1 = linha.indexOf(',');
                int p2 = linha.indexOf(',', p1 + 1);
                int p3 = linha.indexOf(',', p2 + 1);
                if (p3 < 0) continue;

                String sessao = linha.substring(p2 + 1, p3);
                if (!sessao.equals(sessionId)) continue;

                String evento = linha.substring(p3 + 1);
                if (!evento.isEmpty()) fila.add(evento);
            }
        }

        while (!fila.isEmpty()) result.add(fila.poll());
        return result.isEmpty() ? Collections.emptyList() : result;
    }


    @Override
    public List<Alerta> priorizarAlertas(String arquivo, int n) throws IOException {
        if (n <= 0) return Collections.emptyList();

        PriorityQueue<Alerta> fila = new PriorityQueue<>(Comparator.comparingInt(Alerta::getSeverityLevel));
        List<Alerta> result = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo), 1_048_576)) {
            String linha = br.readLine();
            if (linha == null) return Collections.emptyList();

            while ((linha = br.readLine()) != null) {
                String[] c = linha.split(",");
                if (c.length < 7) continue;

                try {
                    Alerta a = new Alerta(
                            Long.parseLong(c[0]),
                            c[1], c[2], c[3], c[4],
                            Integer.parseInt(c[5]),
                            Long.parseLong(c[6])
                    );

                    if (fila.size() < n) fila.add(a);
                    else if (a.getSeverityLevel() > fila.peek().getSeverityLevel()) {
                        fila.poll();
                        fila.add(a);
                    }

                } catch (NumberFormatException ignored) {}
            }
        }

        while (!fila.isEmpty()) result.add(fila.poll());
        Collections.reverse(result);
        return result;
    }

    @Override
    public Map<Long, Long> encontrarPicosTransferencia(String caminhoArquivo) throws IOException {
        File f = new File(caminhoArquivo);
        if (!f.exists() || !f.isFile()) return Collections.emptyMap();

        Map<Long, Long> mapa = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(f), 1024 * 1024)) {
            String linha = br.readLine();
            if (linha == null) return Collections.emptyMap();

            while ((linha = br.readLine()) != null) {
                String[] c = linha.split(",");
                if (c.length < 7) continue;

                try {
                    long timestamp = Long.parseLong(c[0]);
                    long bytes = Long.parseLong(c[6]); // CORRETO

                    mapa.put(timestamp, mapa.getOrDefault(timestamp, 0L) + bytes);
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }

        return mapa.isEmpty() ? Collections.emptyMap() : mapa;
    }

    @Override
    public Optional<List<String>> rastrearContaminacao(String caminhoArquivo, String recursoInicial, String recursoAlvo) throws IOException {
        File f = new File(caminhoArquivo);
        if (!f.exists() || !f.isFile()) return Optional.empty();

        Map<String, Set<String>> grafo = new HashMap<>();
        Map<String, String> ultimo = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(f), 1024 * 1024)) {
            String linha = br.readLine();
            if (linha == null) return Optional.empty();

            String l;
            while ((l = br.readLine()) != null) {
                String[] c = l.split(",");
                if (c.length < 7) continue;

                String sessao = c[2];
                String recurso = c[4];

                if (ultimo.containsKey(sessao)) {
                    String anterior = ultimo.get(sessao);
                    grafo.computeIfAbsent(anterior, k -> new HashSet<>()).add(recurso);
                }

                ultimo.put(sessao, recurso);
            }
        }

        if (!grafo.containsKey(recursoInicial)) return Optional.empty();

        Queue<List<String>> fila = new ArrayDeque<>();
        Set<String> visitados = new HashSet<>();

        fila.add(Collections.singletonList(recursoInicial));
        visitados.add(recursoInicial);

        while (!fila.isEmpty()) {
            List<String> caminho = fila.poll();
            String atual = caminho.get(caminho.size() - 1);

            if (atual.equals(recursoAlvo)) return Optional.of(caminho);

            for (String next : grafo.getOrDefault(atual, Collections.emptySet())) {
                if (!visitados.contains(next)) {
                    visitados.add(next);
                    List<String> novo = new ArrayList<>(caminho);
                    novo.add(next);
                    fila.add(novo);
                }
            }
        }

        return Optional.empty();
    }
}