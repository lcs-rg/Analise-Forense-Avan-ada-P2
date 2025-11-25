import br.edu.icev.aed.forense.AnaliseForenseAvancada;
import br.edu.icev.aed.forense.LucasAnaliseForense;
import br.edu.icev.aed.forense.Alerta;

import java.util.*;

public class TesteValidacao {
    public static void main(String[] args) {
        System.out.println("=== Iniciando Teste de Validação ===\n");

        // Caminho do arquivo CSV - ajuste se necessário
        String arquivoCSV = "teste/arquivo_logs.csv";

        try {
            // Simula o que o validador fará
            System.out.println("📦 Instanciando a classe...");
            AnaliseForenseAvancada impl = new LucasAnaliseForense();
            System.out.println("✅ Classe instanciada com sucesso!\n");

            // Teste 1: Sessões Inválidas
            System.out.println("🔍 Teste 1: Encontrar Sessões Inválidas");
            try {
                Set<String> invalidas = impl.encontrarSessoesInvalidas(arquivoCSV);
                System.out.println("✅ Método executado!");
                System.out.println("   Sessões inválidas encontradas: " + invalidas.size());
                if (!invalidas.isEmpty()) {
                    System.out.println("   Exemplos: " + invalidas);
                }
            } catch (Exception e) {
                System.out.println("❌ Erro: " + e.getMessage());
            }
            System.out.println();

            // Teste 2: Reconstruir Linha do Tempo
            System.out.println("🔍 Teste 2: Reconstruir Linha do Tempo");
            try {
                List<String> timeline = impl.reconstruirLinhaTempo(arquivoCSV, "session-a-01");
                System.out.println("✅ Método executado!");
                System.out.println("   Eventos encontrados: " + timeline.size());
                if (!timeline.isEmpty()) {
                    System.out.println("   Primeiro evento: " + timeline.get(0));
                }
            } catch (Exception e) {
                System.out.println("❌ Erro: " + e.getMessage());
            }
            System.out.println();

            // Teste 3: Priorizar Alertas
            System.out.println("🔍 Teste 3: Priorizar Alertas");
            try {
                List<Alerta> alertas = impl.priorizarAlertas(arquivoCSV, 5);
                System.out.println("✅ Método executado!");
                System.out.println("   Alertas retornados: " + alertas.size());
            } catch (Exception e) {
                System.out.println("❌ Erro: " + e.getMessage());
            }
            System.out.println();

            // Teste 4: Picos de Transferência
            System.out.println("🔍 Teste 4: Encontrar Picos de Transferência");
            try {
                Map<Long, Long> picos = impl.encontrarPicosTransferencia(arquivoCSV);
                System.out.println("✅ Método executado!");
                System.out.println("   Picos encontrados: " + picos.size());
            } catch (Exception e) {
                System.out.println("❌ Erro: " + e.getMessage());
            }
            System.out.println();

            // Teste 5: Rastrear Contaminação
            System.out.println("🔍 Teste 5: Rastrear Contaminação");
            try {
                Optional<List<String>> caminho = impl.rastrearContaminacao(arquivoCSV, "/usr/bin/python", "/var/www/index.html");
                System.out.println("✅ Método executado!");
                if (caminho.isPresent()) {
                    System.out.println("   Caminho encontrado: " + caminho.get());
                } else {
                    System.out.println("   Nenhum caminho encontrado");
                }
            } catch (Exception e) {
                System.out.println("❌ Erro: " + e.getMessage());
            }
            System.out.println();

            System.out.println("=== Teste Concluído ===");
            System.out.println("✅ Seu JAR está funcionando e pode ser importado!");
            System.out.println("✅ Todos os métodos estão acessíveis!");

        } catch (Exception e) {
            System.out.println("❌ ERRO CRÍTICO ao instanciar a classe:");
            e.printStackTrace();
            System.out.println("\n⚠️  Verifique:");
            System.out.println("   1. O JAR está na pasta lib/?");
            System.out.println("   2. O README.txt tem o nome correto da classe?");
            System.out.println("   3. A classe tem construtor público sem parâmetros?");
        }
    }
}